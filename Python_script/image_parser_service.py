# -*- coding: utf-8 -*-
"""
面向科研应用的医学影像可视化平台 - 工业级 Python 引擎
核心升级：引入 SimpleITK 兼容 DICOM (.dcm) 格式
"""

import os
import base64
import threading
import numpy as np
import SimpleITK as sitk
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from io import BytesIO
from PIL import Image
import pydicom

app = FastAPI()

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# --- 配置 ---
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
DATA_DIR = os.path.join(os.path.dirname(BASE_DIR), "med_data")
IMAGE_CACHE = {}
IMAGE_CACHE_LOCK = threading.Lock()
IMAGE_CACHE_MAXSIZE = 3
IMAGE_CACHE_KEYS = []  # FIFO eviction order for LRU approximation

def _cache_put(key, value):
    """Thread-safe cache insert with LRU eviction (caller must hold IMAGE_CACHE_LOCK)."""
    if key in IMAGE_CACHE:
        IMAGE_CACHE_KEYS.remove(key)
    elif len(IMAGE_CACHE) >= IMAGE_CACHE_MAXSIZE:
        oldest = IMAGE_CACHE_KEYS.pop(0)
        IMAGE_CACHE.pop(oldest, None)
    IMAGE_CACHE[key] = value
    IMAGE_CACHE_KEYS.append(key)

def _validate_path(filename: str) -> str:
    """Validate and resolve a file path within DATA_DIR; raise HTTPException on path traversal."""
    if not filename or not filename.strip():
        raise HTTPException(status_code=400, detail="文件名不能为空")
    resolved = os.path.normpath(os.path.join(DATA_DIR, filename))
    norm_data_dir = os.path.normpath(DATA_DIR)
    if not resolved.startswith(norm_data_dir + os.sep) and resolved != norm_data_dir:
        raise HTTPException(status_code=403, detail="禁止访问指定路径")
    return resolved

# 默认的 Mask 类别颜色映射 (RGB 0-255)
# 可以根据实际的 Mask 类别和需求进行调整
# 类别 0 通常是背景，不着色
COLOR_MAP = {
    1: (255, 0, 0),    # 类别 1: 红色 (例如: 肿瘤)
    2: (0, 255, 0),    # 类别 2: 绿色 (例如: 囊肿)
    3: (0, 0, 255),    # 类别 3: 蓝色 (例如: 血管)
    4: (255, 255, 0),  # 类别 4: 黄色
    5: (0, 255, 255),  # 类别 5: 青色
    6: (255, 0, 255),  # 类别 6: 品红色
}

def apply_windowing(slice_arr, ww, wl):
    """
    标准窗宽窗位转换算法。
    当 ww <= 0 时回退到自动对比度拉伸（Min-Max Normalization）。
    """
    if ww <= 0:
        c_min = slice_arr.min()
        c_max = slice_arr.max()
        if c_max == c_min:
            return np.zeros_like(slice_arr, dtype=np.uint8)
        slice_arr = ((slice_arr - c_min) / (c_max - c_min) * 255)
        return slice_arr.astype(np.uint8)

    lower = wl - ww / 2
    upper = wl + ww / 2
    slice_arr = np.clip(slice_arr, lower, upper)
    slice_arr = ((slice_arr - lower) / ww * 255)
    return slice_arr.astype(np.uint8)

def read_dicom_series(folder_path: str):
    """
    安全读取整个 DICOM 文件夹，将其拼装为 3D Volume 并转换为 HU 值
    """
    reader = sitk.ImageSeriesReader()
    dicom_names = reader.GetGDCMSeriesFileNames(folder_path)
    
    if not dicom_names:
        raise ValueError(f"文件夹中未找到 DICOM 序列: {folder_path}")

    reader.SetFileNames(dicom_names)
    
    # 配置读取器以加载 DICOM 元数据 (例如 Rescale Intercept / Slope)
    reader.MetaDataDictionaryArrayUpdateOn()
    reader.LoadPrivateTagsOn()
    
    try:
        image = reader.Execute()
        # SimpleITK ImageSeriesReader with MetaDataDictionaryArrayUpdateOn()
        # already applies RescaleSlope/RescaleIntercept to produce HU values.
        data = sitk.GetArrayFromImage(image)
        return data, image

    except Exception as e:
        raise RuntimeError(f"解析 DICOM 序列失败，可能存在损坏或非图像文件。详细错误: {e}")

def load_medical_image(filename: str):
    """
    增强型加载函数 (支持 NIfTI, DICOM 序列/单文件, NPZ)
    """
    with IMAGE_CACHE_LOCK:
        if filename in IMAGE_CACHE:
            return IMAGE_CACHE[filename]

    full_path = _validate_path(filename)
    if not os.path.exists(full_path):
        raise FileNotFoundError(f"找不到文件或目录: {full_path}")

    print(f"--- 正在加载文件/序列: {filename} ---")
    
    try:
        # 1. 检查是否为 DICOM 序列文件夹
        if os.path.isdir(full_path):
            data, itk_img = read_dicom_series(full_path)
            # 处理维度顺序 (Z, Y, X) -> (X, Y, Z)
            if data.ndim == 3:
                data = np.transpose(data, (2, 1, 0))
            with IMAGE_CACHE_LOCK:
                _cache_put(filename, (data, itk_img))
            return data, itk_img

        # 2. 检查是否为 NPZ
        if filename.lower().endswith('.npz'):
            npz_file = np.load(full_path)
            data_dict = {key: npz_file[key] for key in npz_file.keys()}

            # 对于 NPZ，我们缺乏标准的 spacing 信息，但需要提供默认结构
            # 若 NPZ 内嵌了 affine 矩阵（例如 'affine'），我们可以提取它
            with IMAGE_CACHE_LOCK:
                _cache_put(filename, (data_dict, None))
            return data_dict, None

        # 3. 单文件读取 (NIfTI, MHA, NRRD, 单个 DICOM)
        itk_img = sitk.ReadImage(full_path)
        
        # 检查是否为单张 DICOM 并尝试 HU 校准 (使用 pydicom 辅助)
        if filename.lower().endswith(('.dcm', '.dicom')):
            try:
                ds = pydicom.dcmread(full_path)
                data = ds.pixel_array
                
                # 读取 Rescale 标签
                slope = getattr(ds, 'RescaleSlope', 1)
                intercept = getattr(ds, 'RescaleIntercept', 0)
                
                # 转 HU
                if slope != 1 or intercept != 0:
                     data = data * slope + intercept
                     
                # 重新构建 sitk image 以保持元数据同步
                # 但此处为了简便，我们只处理用于渲染的 data 数组
                
                # pydicom 读取的数据维度可能是 2D (Y, X)，我们需要统一到 3D (X, Y, 1)
                if data.ndim == 2:
                    data = data.T # 转为 (X, Y)
                    data = np.expand_dims(data, axis=-1) # (X, Y, 1)
                    
            except Exception as e:
                 print(f"pydicom 解析单张 DICOM 警告: {e}，回退至 SimpleITK")
                 data = sitk.GetArrayFromImage(itk_img)
                 if data.ndim == 2:
                     data = data.T
                     data = np.expand_dims(data, axis=-1)
                 elif data.ndim == 3:
                     data = np.transpose(data, (2, 1, 0))
        else:
            # 其他 3D 格式 (NIfTI 等)
            data = sitk.GetArrayFromImage(itk_img)
            if data.ndim == 3:
                data = np.transpose(data, (2, 1, 0))
            elif data.ndim == 4:
                data = np.transpose(data, (3, 2, 1, 0))
            
        with IMAGE_CACHE_LOCK:
            _cache_put(filename, (data, itk_img))
        return data, itk_img
        
    except Exception as e:
        print(f"加载失败: {e}")
        raise

def get_main_array_from_loaded_data(loaded_data):
    """ 从 load_medical_image 的返回中获取主图像数组 """
    if isinstance(loaded_data, dict): # NPZ文件
        # 尝试获取 'image' 或 'data' 键，否则取第一个数组
        if 'image' in loaded_data:
            return loaded_data['image']
        elif 'data' in loaded_data:
            return loaded_data['data']
        elif len(loaded_data) > 0:
            return loaded_data[list(loaded_data.keys())[0]]
        else:
            raise ValueError("NPZ文件不包含任何数组")
    else: # 其他格式
        return loaded_data

def get_mask_array_from_loaded_data(loaded_data):
    """ 从 load_medical_image 的返回中获取 Mask 数组 """
    if isinstance(loaded_data, dict): # NPZ文件
        # 尝试获取 'mask' 或 'label' 键
        if 'mask' in loaded_data:
            return loaded_data['mask']
        elif 'label' in loaded_data:
            return loaded_data['label']
        else:
            # 如果没有明确的mask/label键，可以根据需要返回None或抛出错误
            print("NPZ文件未找到明确的 'mask' 或 'label' 数组。")
            return None
    else: # 其他格式，目前不支持直接从非NPZ文件加载mask
        return None

def overlay_masks_on_slice(image_slice: np.ndarray, mask_slice: np.ndarray, alpha: float = 0.4, color_map: dict = COLOR_MAP) -> np.ndarray:
    """
    将多类别 Mask 半透明叠加到灰度图像切片上。
    """
    if image_slice.shape != mask_slice.shape:
        raise ValueError("图像切片和 Mask 切片的形状必须一致。")

    # 确保图像切片是 0-255 的 uint8 格式
    if image_slice.dtype != np.uint8:
        image_slice = apply_windowing(image_slice, 0, 0) # 强制自动窗宽窗位

    # 将灰度图像转换为 RGB 图像，以便叠加颜色
    rgb_image = np.stack([image_slice, image_slice, image_slice], axis=-1) # (H, W, 3)

    # 创建一个与图像大小相同的全黑 RGB 叠加层
    overlay = np.zeros_like(rgb_image, dtype=np.uint8)

    # 遍历 Mask 中的所有唯一类别 (排除背景 0)
    unique_labels = np.unique(mask_slice)
    for label in unique_labels:
        if label == 0: # 假设 0 是背景，不着色
            continue
        
        if label in color_map:
            color = color_map[label]
            # 找到当前类别的所有像素
            mask_pixels = (mask_slice == label)
            # 将对应像素设置为指定颜色
            overlay[mask_pixels] = color
        else:
            print(f"警告: Mask 类别 {label} 未在 COLOR_MAP 中定义，将跳过此类别。")

    # 执行 Alpha 混合
    blended_image = (alpha * overlay + (1 - alpha) * rgb_image).astype(np.uint8)

    return blended_image


@app.get("/api/inspect")
def inspect_image(filename: str):
    try:
        loaded_data, itk_img = load_medical_image(filename)
        
        # 从加载的数据中获取主图像数组进行检查
        data = get_main_array_from_loaded_data(loaded_data)

        min_val = data.min()
        max_val = data.max()
        modality = "MR" 
        if min_val < -500 and max_val > 500:
            modality = "CT" 
        
        spacing = [1.0, 1.0, 1.0]
        direction = [1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0]

        if itk_img is not None: # 只有SimpleITK加载的才有itk_img
            spacing = list(itk_img.GetSpacing())
            direction = list(itk_img.GetDirection())
        elif isinstance(loaded_data, dict) and 'spacing' in loaded_data:
            # 尝试从 NPZ 中提取元数据 (如果保存时加入了的话)
            spacing = loaded_data['spacing'].tolist()
        
        format_str = "NIfTI"
        if filename.lower().endswith('.dcm') or os.path.isdir(os.path.join(DATA_DIR, filename)):
            format_str = "DICOM"
        elif filename.lower().endswith('.npz'):
            format_str = "NPZ"

        return {
            "status": "success",
            "format": format_str,
            "shape": list(data.shape),
            "spacing": spacing,                
            "direction": direction,
            "modality": modality,              
            "msg": "解析成功"
        }
    except Exception as e:
        print(f"Error inspecting image: {e}") 
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/api/slice")
def get_slice(
    filename: str,
    index: int = 0,
    axis: str = "axial",     # 新增支持: axial(横断面), coronal(冠状面), sagittal(矢状面)
    ww: float = 400,
    wl: float = 40,
    mask_filename: str = None, 
    alpha: float = 0.4         
):
    try:
        loaded_data, _ = load_medical_image(filename)
        data = get_main_array_from_loaded_data(loaded_data)

        # 剥离多余通道 (例如颜色通道)，只保留 (X, Y, Z)
        if len(data.shape) == 4:
            data = data[:, :, :, 0] 

        # 根据指定的平面截取切片
        # 当前我们的 3D numpy array 约定格式为 (X, Y, Z) 
        if axis == "axial":
            # 横断面，沿 Z 轴切取，平面为 X-Y
            max_idx = data.shape[2] - 1
            safe_index = max(0, min(index, max_idx))
            image_slice = data[:, :, safe_index]
        elif axis == "coronal":
            # 冠状面，沿 Y 轴切取，平面为 X-Z
            max_idx = data.shape[1] - 1
            safe_index = max(0, min(index, max_idx))
            image_slice = data[:, safe_index, :]
        elif axis == "sagittal":
            # 矢状面，沿 X 轴切取，平面为 Y-Z
            max_idx = data.shape[0] - 1
            safe_index = max(0, min(index, max_idx))
            image_slice = data[safe_index, :, :]
        else:
            raise ValueError(f"不支持的切面类型: {axis}")

        # 调整方向，匹配可视化需求 (大部分图像需要旋转 90 度以适应常规的屏幕坐标系)
        image_slice = np.rot90(image_slice)

        processed_image_slice = apply_windowing(image_slice, ww, wl) 

        final_image_to_render = processed_image_slice

        # 相同的逻辑应用于 Mask
        if mask_filename:
            mask_loaded_data, _ = load_medical_image(mask_filename)
            mask_data = get_mask_array_from_loaded_data(mask_loaded_data)
            
            if mask_data is None:
                raise HTTPException(status_code=400, detail=f"Mask文件 '{mask_filename}' 未包含可识别的 Mask 数组。")

            if len(mask_data.shape) == 4:
                mask_data = mask_data[:, :, :, 0]
            
            # 使用相同的切面和索引提取 Mask
            if axis == "axial":
                mask_max_idx = mask_data.shape[2] - 1
                mask_safe_index = max(0, min(index, mask_max_idx))
                mask_slice = mask_data[:, :, mask_safe_index]
            elif axis == "coronal":
                mask_max_idx = mask_data.shape[1] - 1
                mask_safe_index = max(0, min(index, mask_max_idx))
                mask_slice = mask_data[:, mask_safe_index, :]
            elif axis == "sagittal":
                mask_max_idx = mask_data.shape[0] - 1
                mask_safe_index = max(0, min(index, mask_max_idx))
                mask_slice = mask_data[mask_safe_index, :, :]

            mask_slice = np.rot90(mask_slice)

            # 叠加 Mask
            final_image_to_render = overlay_masks_on_slice(
                processed_image_slice, 
                mask_slice,
                alpha=alpha,
                color_map=COLOR_MAP
            )
            pil_img = Image.fromarray(final_image_to_render).convert("RGB")
        else:
            pil_img = Image.fromarray(final_image_to_render).convert("L")

        buffered = BytesIO()
        pil_img.save(buffered, format="PNG")

        return {"image": base64.b64encode(buffered.getvalue()).decode()}
    except Exception as e:
        print(f"切片提取或Mask叠加失败: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    import uvicorn
    try:
        import SimpleITK
        import pydicom
    except ImportError:
        print("请在控制台执行: pip install SimpleITK pydicom")

    print(f"全能工人就绪 (支持 NIfTI + DICOM Series + NPZ, 包含多切面渲染)...")
    uvicorn.run(app, host="127.0.0.1", port=8000)