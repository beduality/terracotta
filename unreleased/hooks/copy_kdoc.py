import os
import shutil

def on_post_build(config):
    # Search for KDocs in potential locations
    potential_folders = [
        'build/dokka/htmlMultiModule',
        'build/dokka/html',
        'modules/terracotta-core/build/dokka/html'
    ]
    
    source_folder = None
    for folder in potential_folders:
        if os.path.exists(folder):
            source_folder = folder
            break
            
    target_folder = os.path.join(config['site_dir'], 'apidocs')
    
    if source_folder:
        if os.path.exists(target_folder):
            shutil.rmtree(target_folder)
        shutil.copytree(source_folder, target_folder)
        print(f"Copied KDoc from {source_folder} to {target_folder}")
    else:
        print("Warning: No KDoc folder found.")
