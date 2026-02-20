#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
Download gradle-wrapper.jar from a GitHub Release asset.

Usage:
  scripts/download-gradle-wrapper-jar.sh --repo <owner/repo> --tag <release-tag> [--asset <name>] [--output <path>]

Options:
  --repo    GitHub repository in owner/repo format (required)
  --tag     Release tag that contains the wrapper jar asset (required)
  --asset   Asset filename (default: gradle-wrapper.jar)
  --output  Output path (default: gradle/wrapper/gradle-wrapper.jar)
  -h, --help Show this help
USAGE
}

repo=""
tag=""
asset="gradle-wrapper.jar"
output="gradle/wrapper/gradle-wrapper.jar"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --repo)
      repo="${2:-}"
      shift 2
      ;;
    --tag)
      tag="${2:-}"
      shift 2
      ;;
    --asset)
      asset="${2:-}"
      shift 2
      ;;
    --output)
      output="${2:-}"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage >&2
      exit 1
      ;;
  esac
done

if [[ -z "$repo" || -z "$tag" ]]; then
  echo "Both --repo and --tag are required." >&2
  usage >&2
  exit 1
fi

mkdir -p "$(dirname "$output")"

url="https://github.com/${repo}/releases/download/${tag}/${asset}"
echo "Downloading ${url}"
curl -fL "$url" -o "$output"

echo "Saved ${output}"
