HUONG DAN CHAY JAVA 25 VOI ONNX RUNTIME

Ban nay da chinh de ung dung khong bi tat neu ONNX Runtime khong load duoc DLL.
Neu ONNX load loi, dashboard van chay bang fallback forecast.

De ONNX Runtime co the chay thuc su tren Java 25:
1. Run -> Edit Configurations -> VM options:
   --enable-native-access=ALL-UNNAMED

2. Cai Microsoft Visual C++ Redistributable 2015-2022 x64.

3. Dam bao Project SDK va Run JRE deu la Java 25, sau do xoa target va build lai.

4. Neu van loi onnxruntime.dll, do la loi native DLL tren may Windows, khong phai loi code Java hay model ONNX.

Da them san:
- .mvn/jvm.config
- spring-boot-maven-plugin jvmArguments
- .run/WebBanDoGiaDungApplication_Java25_ONNX.run.xml
- LstmRevenueModelService bat Throwable de app khong crash khi DLL loi
