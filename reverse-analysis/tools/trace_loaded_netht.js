console.log("ISSUE5_JAVA_AVAILABLE=" + Java.available);
Java.performNow(function () {
    const target = "com.netease.htprotect.factory.JNIFactory";
    try {
        const factory = Java.use(target);
        const declared = factory.class.getDeclaredMethods();
        const methods = [];
        for (let i = 0; i < declared.length; i++) {
            methods.push(declared[i].toString());
        }
        console.log("ISSUE5_JNI_METHODS=" + JSON.stringify(methods));
        console.log("ISSUE5_JNI_LOADER=" + factory.class.getClassLoader());
    } catch (error) {
        console.log("ISSUE5_JNI_ERROR=" + error);
    }

});
