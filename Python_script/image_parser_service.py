# -*- coding: utf-8 -*-
"""
面向科研应用的医学影像可视化平台 - 工业级 Python 引擎
核心升级：加入内存缓存 (LRU Cache)，支持大文件（如 LiTS volume-0.nii）流畅秒开
"""

import os
import base64
import nibabel as nib
import numpy as np
from fastapi import FastAPI, HTTPException, Query
from fastapi.middleware.cors import CORSMiddleware
from io import BytesIO
from PIL import Image
from cachetools import LRUCache  # 如果报错，请在终端执行: pip install cachetools

app = FastAPI()

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# --- 配置 ---
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
# 向上跳一级找到 med_data 目录
DATA_DIR = os.path.join(os.path.dirname(BASE_DIR), "med_data")

# 【核心升级】内存缓存：最多缓存 3 个大型 3D 影像对象
# 这样切换切片时不需要重复读取磁盘，速度提升 100 倍
IMAGE_CACHE = LRUCache(maxsize=3)

def apply_windowing(slice_arr, ww, wl):
    """ 窗宽窗位转换算法 """
    min_val = wl - ww / 2
    max_val = wl + ww / 2
    slice_arr = np.clip(slice_arr, min_val, max_val)
    slice_arr = ((slice_arr - min_val) / ww * 255).astype(np.uint8)
    return slice_arr

def load_medical_image(filename: str):
    """ 带缓存的加载函数 """
    if filename in IMAGE_CACHE:
        return IMAGE_CACHE[filename]

    full_path = os.path.join(DATA_DIR, filename)
    if not os.path.exists(full_path):
        raise FileNotFoundError(f"找不到文件: {full_path}")

    print(f"--- 正在加载大型文件: {filename} ---")
    img = nib.load(full_path)
    img = nib.as_closest_canonical(img)
    data = img.get_fdata() # 读取数据到内存
    IMAGE_CACHE[filename] = data
    return data

@app.get("/api/inspect")
def inspect_image(filename: str):
    try:
        data = load_medical_image(filename)
        return {
            "status": "success",
            "format": "NIfTI",
            "shape": list(data.shape),
            "msg": "解析成功"
        }
    except Exception as e:
        raise HTTPException(status_code=404, detail=str(e))

@app.get("/api/slice")
def get_slice(
    filename: str,
    index: int = 0,
    axis: str = "axial",
    ww: float = 400,
    wl: float = 40
):
    try:
        # 1. 从缓存获取数据，不重复读磁盘
        img_data = load_medical_image(filename)

        # 2. 索引安全检查
        # LiTS 数据通常在第 3 个维度 (2) 是切片层数
        max_idx = img_data.shape[2] - 1
        safe_index = max(0, min(index, max_idx))

        # 3. 提取切片 (默认横断面)
        slice_arr = img_data[:, :, safe_index]

        # 4. 旋转与翻转（使其符合放射学观察习惯）
        slice_arr = np.rot90(slice_arr)

        # 5. 调窗处理
        processed = apply_windowing(slice_arr, ww, wl)

        # 6. 转 Base64
        pil_img = Image.fromarray(processed).convert("L")
        buffered = BytesIO()
        pil_img.save(buffered, format="PNG")

        return {"image": base64.b64encode(buffered.getvalue()).decode()}
    except Exception as e:
        print(f"切片提取失败: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    import uvicorn
    # 自动安装缺失的库提示
    try:
        import cachetools
    except ImportError:
        print("请在控制台执行: pip install cachetools")

    print(f"工人就绪，正在监听大文件请求...")
    uvicorn.run(app, host="127.0.0.1", port=8000)