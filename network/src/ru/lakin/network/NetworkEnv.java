package ru.lakin.network;
import java.io.File;

// =========== Переменные окружения класса NetworkEnv (MyEnv) ==========================================================
// =====================================================================================================================
    public class NetworkEnv {
// ---------------- CR платформы
    public static final String myCR = System.lineSeparator();
// ---------------- слэш пути патформы (прямой|обратный)
    public static final String mySL = File.separator;
// ---------------- домашняя папка программы (!!!!! ЗАГЛУШКА)
//  public static final String myHomePath = System.getProperty("user.dir");
    public static final String myHomePath = "D:" + mySL + "__Android" + mySL + "TestHome";
// ---------------- путь и имя логфайла
    public static String myLogFilePathName = myHomePath + File.separator + "Log.txt";
// ---------------- IP сервера для написания и отладки клиент-серверных приложений. (!!!!! ЗАГЛУШКА)
    public static final String IP_ADDR = "127.0.0.1";
    public static final int PORT = 8187;
}
