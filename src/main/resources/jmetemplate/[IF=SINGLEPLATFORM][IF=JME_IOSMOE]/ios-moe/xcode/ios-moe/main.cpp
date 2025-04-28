#include <MOE/MOE.h>
#include <string.h>
#include <iostream>

int main(int argc, char *argv[]) {
    int jvm_argc = argc+1;
    char** jvm_argv = new char*[jvm_argc];

    /*
    Sample memory sizes:
        268435456 - 256Mb
        536870912 - 512Mb
        1073741824 - 1G
        1610612736 - 1.5G
        2147483648 - 2G
    */

    jvm_argv[0]=argv[0];
    jvm_argv[1]=strdup("-Xmx1073741824"); //1G
    // Note: dalvik VM doesn't support -XX:MaxDirectMemorySize but Xmx is enough
    for(int i=1;i<argc;++i)
    {
        jvm_argv[i+1]=argv[i];
    }

    std::cout << "args: " << jvm_argc <<std::endl;
    for(int i=0;i<jvm_argc;++i)
    {
        std::cout << jvm_argv[i] << std::endl;
    }

    return moevm(jvm_argc, jvm_argv);
}

