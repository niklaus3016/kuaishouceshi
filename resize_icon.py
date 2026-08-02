#!/usr/bin/env python3
import os
import sys

# 检查是否有PIL可用
try:
    from PIL import Image
    has_pil = True
except ImportError:
    has_pil = False

# 图标尺寸要求
icon_sizes = {
    'mipmap-mdpi': 48,
    'mipmap-hdpi': 72,
    'mipmap-xhdpi': 96,
    'mipmap-xxhdpi': 144,
    'mipmap-xxxhdpi': 192
}

# 源图片路径
source_image = 'yxxzicon512.png'
res_dir = 'android/app/src/main/res'

if not os.path.exists(source_image):
    print(f"错误：找不到源图片 {source_image}")
    sys.exit(1)

if has_pil:
    print("使用PIL处理图标...")
    try:
        # 打开源图片
        img = Image.open(source_image)
        
        # 为每个尺寸生成图标
        for folder, size in icon_sizes.items():
            target_dir = os.path.join(res_dir, folder)
            if not os.path.exists(target_dir):
                os.makedirs(target_dir)
            
            # 调整图片大小
            resized = img.resize((size, size), Image.Resampling.LANCZOS)
            
            # 保存图标
            target_path = os.path.join(target_dir, 'ic_launcher.png')
            resized.save(target_path, 'PNG')
            
            # 保存圆角图标
            round_target_path = os.path.join(target_dir, 'ic_launcher_round.png')
            resized.save(round_target_path, 'PNG')
            
            print(f"已生成: {target_path} ({size}x{size})")
            print(f"已生成: {round_target_path} ({size}x{size})")
        
        print("图标处理完成！")
    except Exception as e:
        print(f"处理图标时出错: {e}")
        sys.exit(1)
else:
    print("警告：没有安装PIL库，无法调整图标尺寸")
    print("将直接复制原始图标到所有目录...")
    
    # 直接复制原始图标
    for folder in icon_sizes.keys():
        target_dir = os.path.join(res_dir, folder)
        if not os.path.exists(target_dir):
            os.makedirs(target_dir)
        
        target_path = os.path.join(target_dir, 'ic_launcher.png')
        round_target_path = os.path.join(target_dir, 'ic_launcher_round.png')
        
        import shutil
        shutil.copy(source_image, target_path)
        shutil.copy(source_image, round_target_path)
        
        print(f"已复制: {target_path}")
        print(f"已复制: {round_target_path}")
    
    print("图标复制完成（注意：所有尺寸使用相同图片，建议优化）")