# -*- coding: utf-8 -*-
"""
面向科研应用的医学影像可视化平台 - 工业级 Python 引擎
核心升级：引入 SimpleITK 兼容 DICOM (.dcm) 格式
"""

import os
import base64
import numpy as np
import SimpleITK as sitk # 需要安装: pip install SimpleITK
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from io import BytesIO
from PIL import Image
from cachetools import LRUCache 

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
IMAGE_CACHE = LRUCache(maxsize=3)

def apply_windowing(slice_arr, ww, wl):
    """ 
    智能窗宽窗位转换算法 (回归自动版)
    优先保证图像可见性，忽略传入的 ww/wl，强制自动拉伸
    """
    c_min = slice_arr.min()
    c_max = slice_arr.max()
    
    # 如果整张切片是完全空白的，直接返回纯黑
    if c_max == c_min:
        return np.zeros_like(slice_arr, dtype=np.uint8)

    # 强制自动对比度拉伸 (Auto Contrast / Min-Max Normalization)
    # 将当前切片的像素值线性拉伸到 0-255 范围
    # 这样无论原图是 CT、MRI 还是 Mask，都能看清结构
    slice_arr = ((slice_arr - c_min) / (c_max - c_min) * 255)
    
    return slice_arr.astype(np.uint8)

def load_medical_image(filename: str):
    """ 通用加载函数 (支持 NIfTI 和 DICOM) """
    if filename in IMAGE_CACHE:
        return IMAGE_CACHE[filename]

    full_path = os.path.join(DATA_DIR, filename)
    if not os.path.exists(full_path):
        raise FileNotFoundError(f"找不到文件: {full_path}")

    print(f"--- 正在加载文件: {filename} ---")
    
    try:
        # 使用 SimpleITK 读取
        itk_img = sitk.ReadImage(full_path)
        data = sitk.GetArrayFromImage(itk_img)
        
        # 处理维度顺序 (Z, Y, X) -> (X, Y, Z)
        if data.ndim == 3:
            data = np.transpose(data, (2, 1, 0))
        elif data.ndim == 4:
            data = np.transpose(data, (3, 2, 1, 0))
            
        IMAGE_CACHE[filename] = (data, itk_img) 
        return data, itk_img
        
    except Exception as e:
        print(f"加载失败: {e}")
        raise e

@app.get("/api/inspect")
def inspect_image(filename: str):
    try:
        data, itk_img = load_medical_image(filename)
        
        min_val = data.min()
        max_val = data.max()
        modality = "MR" 
        if min_val < -500 and max_val > 500:
            modality = "CT" 
        
        spacing = list(itk_img.GetSpacing())
        direction = list(itk_img.GetDirection())
            
        return {
            "status": "success",
            "format": "DICOM" if filename.lower().endswith('.dcm') else "NIfTI",
            "shape": list(data.shape),
            "spacing": spacing,                
            "affine": direction,
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
    axis: str = "axial",
    ww: float = 400,
    wl: float = 40
):
    try:
        data, _ = load_medical_image(filename)

        if len(data.shape) == 4:
            data = data[:, :, :, 0] 

        max_idx = data.shape[2] - 1
        safe_index = max(0, min(index, max_idx))

        slice_arr = data[:, :, safe_index]
        slice_arr = np.rot90(slice_arr)

        processed = apply_windowing(slice_arr, ww, wl)

        pil_img = Image.fromarray(processed).convert("L")
        buffered = BytesIO()
        pil_img.save(buffered, format="PNG")

        return {"image": base64.b64encode(buffered.getvalue()).decode()}
    except Exception as e:
        print(f"切片提取失败: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    import uvicorn
    try:
        import SimpleITK
    except ImportError:
        print("请在控制台执行: pip install SimpleITK")

    print(f"全能工人就绪 (支持 NIfTI + DICOM)...")
    uvicorn.run(app, host="127.0.0.1", port=8000)