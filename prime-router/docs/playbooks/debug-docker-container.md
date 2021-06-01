# How To Debug in Your Docker Container

## Intro
This playbook is a quick run down of how to connect and debug in your Docker container when coding in Intellij.

## Steps
1. In Intellij, click on Run, and select Edit Configurations ![](../assets/playbook-debug-docker-container/1-EditConfigurations.png)
2. Create a new Remote JVM Debug ![](../assets/playbook-debug-docker-container/2-CreateRemoteJvmDebug.png)
3. Set up the configuration for the remote JVM debug to look like this. In particular, you want to attach to the remote JVM, and you want to set the module classpath to `prime-router` so the debugger knows where to attach ![](../assets/playbook-debug-docker-container/3-RemoteDebugConfig.png)
4. In your code, set your breakpoint, and then start your docker container with `docker-compose up`
5. Once your docker container is running, in order to attach, select Run again. ![](../assets/playbook-debug-docker-container/5-SelectRun.png)
6. Select Debug (not Attach to Process) ![](../assets/playbook-debug-docker-container/6-SelectDebug.png)
7. Select your Docker Debug that you set up in step 3 ![](../assets/playbook-debug-docker-container/7-SelectDockerDebug.png)
8. A console window will pop up that will show you that it is connected to Docker, and at that point, you can interact with your container and then step through the code at your breakpoints. ![](../assets/playbook-debug-docker-container/8-ConsoleWindow.png)

## Conclusion
That's it. Good luck.