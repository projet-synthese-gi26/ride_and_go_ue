import os
import re

# Extensions pertinentes pour un projet Java Spring
INCLUDED_EXTENSIONS = (
    ".java",
    ".properties",
    ".yml",
    ".yaml",
    ".txt",
    "Dockerfile"
)

# Dossiers / fichiers à ignorer (par nom ou préfixe)
IGNORED_PREFIXES = (
    ".git",
    ".idea",
    ".vscode",
    "target",
    "build",
    "__pycache__"
)

# Extensions explicitement ignorées
IGNORED_EXTENSIONS = (
    ".class",
    ".jar",
    ".war",
    ".ear",
    ".log",
    ".iml"
)

# Fichiers de configuration ou techniques à exclure
CONFIG_FILE_PATTERNS = [
    re.compile(r"^pom\.xml$"),          # Maven (optionnel)
    re.compile(r"^build\.gradle.*"),    # Gradle (optionnel)
    re.compile(r"^gradlew.*"),
    re.compile(r"^settings\.gradle.*"),
    re.compile(r"^\.?env.*"),
]

def is_config_file(filename: str) -> bool:
    return any(pattern.match(filename) for pattern in CONFIG_FILE_PATTERNS)

def should_skip(path: str, filename: str) -> bool:
    parts = path.split(os.sep)

    # Ignore dossiers techniques
    if any(part.startswith(IGNORED_PREFIXES) for part in parts):
        return True
    if filename.startswith(IGNORED_PREFIXES):
        return True

    # Ignore extensions non pertinentes
    if filename.endswith(IGNORED_EXTENSIONS):
        return True

    # Ignore certains fichiers de config
    if is_config_file(filename):
        return True

    # Inclure uniquement les extensions utiles
    if not filename.endswith(INCLUDED_EXTENSIONS):
        return True

    return False

def concat_files(root_dir: str, output_file: str):
    with open(output_file, "w", encoding="utf-8") as outfile:
        for dirpath, _, filenames in os.walk(root_dir):
            for filename in sorted(filenames):
                if should_skip(dirpath, filename):
                    continue

                file_path = os.path.join(dirpath, filename)
                try:
                    with open(file_path, "r", encoding="utf-8") as infile:
                        relative_path = os.path.relpath(file_path, root_dir)
                        outfile.write(f"\n--- FILE: {relative_path} ---\n")
                        outfile.write(infile.read())
                        outfile.write("\n")
                except Exception as e:
                    print(f"❌ Erreur lecture {file_path} : {e}")

    print(f"\n✅ Fichier généré : {output_file}")

# Exemple d'utilisation
if __name__ == "__main__":
    dossier_source = "."   # racine du projet Spring
    fichier_sortie = "./spring-project-2.txt"
    concat_files(dossier_source, fichier_sortie)