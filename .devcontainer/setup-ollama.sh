#!/bin/bash
set -e

echo "Installing Ollama..."

# Download and install Ollama
curl -fsSL https://ollama.ai/install.sh | sh

echo "Starting Ollama service..."

# Start Ollama in the background
ollama serve &
OLLAMA_PID=$!

# Wait for Ollama to start
sleep 5

echo "Pulling Qwen 3.8:27b model..."

# Pull the model
ollama pull qwen:27b

echo "✅ Ollama and Qwen 3.8:27b are ready!"
echo "Model is available at: http://localhost:11434"
echo ""
echo "To use the model in your code:"
echo "- API endpoint: http://localhost:11434/api/generate"
echo "- Model name: qwen:27b"
