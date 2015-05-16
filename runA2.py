import subprocess
import time
import os

subprocess.call(['killall java'], shell=True)

subprocess.call(['javac -d bin src/*.java'], shell=True)
time.sleep(1)

os.chdir("./bin/")

# Run name server
subprocess.Popen(['java NameServer 9999'], shell=True)
time.sleep(2)

# Run bank
subprocess.Popen(['java Bank 8888 9999'], shell=True)
time.sleep(2)

# Run Content
subprocess.Popen(['java Content 7777 ../content.txt 9999'], shell=True)
time.sleep(2)

# Run Store
subprocess.Popen(['java Store 6666 ../stock.txt 9999'], shell=True)
time.sleep(2)

# Run Client
subprocess.Popen(['java Client 2 9999'], shell=True)
time.sleep(10)

subprocess.call(['killall java'], shell=True)