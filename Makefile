SHELL := powershell.exe
.SHELLFLAGS := -NoProfile -Command

# Java source files for this project
SRC = Calculator.java Protocol.java ClientHandler.java Server.java Client.java

# Default client settings (you can override these from the command line)
# Example: make run CLIENT_NAME=Bob HOST=127.0.0.1 PORT=6789
CLIENT_NAME ?= Alice
HOST ?= 127.0.0.1
PORT ?= 6789

.PHONY: build server client run clean

# Compile all Java files into .class files
build:
	javac $(SRC)

# Run only the server (blocks this terminal)
server: build
	java Server

# Run only the client (connects to HOST:PORT)
client: build
	java Client $(CLIENT_NAME) $(HOST) $(PORT)

# Run server first, then client:
# 1) open server in a new PowerShell window
# 2) wait 1 second so server can start listening
# 3) run client in current terminal
run: build
	Start-Process powershell -ArgumentList '-NoExit','-Command','cd "$(CURDIR)"; java Server'
	Start-Sleep -Seconds 1
	java Client $(CLIENT_NAME) $(HOST) $(PORT)

# Delete compiled class files
clean:
	Remove-Item -ErrorAction SilentlyContinue *.class
