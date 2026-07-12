import os
import shutil


def on_pre_build(config):
    source = 'LICENSE'
    target = os.path.join(config['docs_dir'], 'LICENSE.md')

    if not os.path.exists(source):
        print(f"Warning: {source} not found.")
        return

    shutil.copy(source, target)
    print(f"Copied {source} to {target}")
