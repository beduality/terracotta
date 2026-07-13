import os
import shutil

KDOC_FOLDERS = [
    'build/dokka/htmlMultiModule',
    'build/dokka/html',
]


def _find_kdoc_source():
    for folder in KDOC_FOLDERS:
        if os.path.exists(folder) and os.path.isdir(folder):
            return folder
    return None


def _copy_kdoc(source, target):
    if os.path.exists(target):
        shutil.rmtree(target)
    shutil.copytree(source, target)
    print(f"Copied KDoc from {source} to {target}")


def on_pre_build(config):
    # Copy generated KDocs into the docs directory so MkDocs can include them in the site
    source_folder = _find_kdoc_source()
    if source_folder:
        target_folder = os.path.join(config['docs_dir'], 'apidocs')
        _copy_kdoc(source_folder, target_folder)
    else:
        print("Warning: No KDoc folder found. Run './gradlew :dokkaHtmlMultiModule' first.")


def on_post_build(config):
    # Also copy KDocs to the final site directory as a fallback
    source_folder = _find_kdoc_source()
    if source_folder:
        target_folder = os.path.join(config['site_dir'], 'apidocs')
        _copy_kdoc(source_folder, target_folder)
