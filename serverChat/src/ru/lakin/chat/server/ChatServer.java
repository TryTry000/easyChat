package ru.lakin.chat.server;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;

import ru.lakin.network.*;
import static ru.lakin.network.NetworkEnv.PORT;

// ChatServer будет слушателем событий соединения. То есть реализует интерфейс TCPConnectionListener
public class ChatServer implements TCPConnectionListener {
    public static void main ( String[] args ) { new ChatServer(); }

    // Соединений может быть много, и нужен список этих соединений
    // Стандартный класс ArrayList реализует список, где можно добавлять и убирать сущности,
    // в нашем случае - TCP-соединения
    private final ArrayList<TCPConnection> connections = new ArrayList<>();

    // Конструктор
    private ChatServer() {
        System.out.println("Сервер стартует, порт " + PORT);
        // Базовый класс ServerSocket умеет слушать порт и принимать входящее соединение. Слушаем по порту PORT.
        // Ниже - синтаксическая конструкция try, которая, при наступлении исключения, сама закрывает ресурс
        try ( ServerSocket serverSocket = new ServerSocket(PORT) ) {
            System.out.println("Сервер пашет...");
            // Сервер слушает. На каждое пришедшее новое входящее соединение создаёт TCPConnection
            // Сервер прост и не рассчитан на сложное внешнее управление
            while (true) {
                // На каждое новое соединение надо создать новый TCPConnection
                try {
                    // Инициатор общения - клиентский Socket. Со стороны сервера он лишь воссоздаётся.
                    // Создать объект типа Socket на стороне клиента и воссоздать его с помощью ServerSocket на стороне
                    // сервера – необходимый минимум для соединения.
                    //
                    // На стороне клиента в виде экземпляра класса Socket объявляется запрос на соединение. Чтобы его
                    // принять и, в свою очередь, воссоздать клиентский Socket на стороне сервера, сервер имеет
                    // стандартный класс ServerSocket, и в нём - метод accept().
                    //
                    // ServerSocket - это, как и Socket, - стандартный класс. Но его природа совершенно иная, нежели у
                    // Socket. ServerSocket нужен лишь на этапе создания соединения. Его метод accept() ждёт, пока
                    // кто-либо захочет подсоединится к серверу. Если дождался, то возвращает объект типа Socket -
                    // воссозданный клиентский сокет. И когда сокет клиента воссоздан на стороне сервера, можно
                    // начинать двухстороннее общение.
                    //
                    // ChatServer - одновременно и чат-сервер, и TCPConnectionListener (через интерфейс)
                    // ChatServer может вызвать соединение, передавая ему:
                    // - себя, как слушателя (eventListener)
                    // - экземпляр сокета, по которому слушаем
                    //
                    // Короче...
                    // Создаём serverSocket (выше)
                    // Этот serverSocket в бесконечном цикле который слушает по порту PORT,
                    // постоянно вися в методе accept. accept ждёт соединения, и как только соединение появилось,
                    // возвращает готовый объект Socket, который пришёл от клиента и связан с этим соединением. И тут же
                    // передаём этот объект Socket и себя, как слушателя, в конструктор класса TCPConnection, создавая
                    // новый экземпляр TCP-соединения.
                    new TCPConnection( this, serverSocket.accept());
                }
                catch (IOException e) { System.out.println("Исключение TCPConnection: " + e); }
            }


        }
        catch ( IOException e ) { throw new RuntimeException(e); }
    }

    // Реализация методов интерфейса TCPConnectionListener: обработка событий.
    // Эти методы синхронизируем, чтобы нельзя было одновременно попасть в них из разных нитей.
    @Override
    // Соединение запустилось, можно работать
    public synchronized void onConnectionReady(TCPConnection tcpConnection) {
        // Когда готово соединение, мы его добавляем в список соединений
        connections.add(tcpConnection);
        // Во все соединения выдаём строку о присоединении клиента.
        // Когда мы складываем TCPConnection со строкой, у экземпляра класса автоматом вызывается
        // метод toString. А он у нас в классе TCPConnection переопредделён и выдаёт интернет-адрес с портом.
        sendToAllConnections("Клиент подключился: " + tcpConnection);
    }
    @Override
    // Приняли входящую строку
    public synchronized void onReceiveString(TCPConnection tcpConnection, String value) {
        // Приняли строчку и рассылаем всем соединениям (клиентам)
        // Когда мы складываем TCPConnection со строкой, у экземпляра класса автоматом вызывается
        // метод toString. А он у нас в классе TCPConnection переопредделён и выдаёт интернет-адрес с портом.
        sendToAllConnections("Клиент " + tcpConnection + " говорит: " + value);
    }
    @Override
    // Соединение разорвалось
    public synchronized void onDisconnect(TCPConnection tcpConnection) {
        // Когда соединение отвалилось, мы его изымаем из списка соединений
        connections.remove(tcpConnection);
        // Во все соединения выдаём строку о том, что клиент отвалился.
        // Когда мы складываем TCPConnection со строкой, у экземпляра класса автоматом вызывается
        // метод toString. А он у нас в классе TCPConnection переопредделён и выдаёт интернет-адрес с портом.
        sendToAllConnections("Клиент отвалился: " + tcpConnection);

    }
    @Override
    // Ошибка соединения
    public synchronized void onException(TCPConnection tcpConnection, Exception e) {
        // Когда случилось исключение, пишем в консоль
        System.out.println("Исключение TCPConnection: " + e);
        connections.remove(tcpConnection);

    }

    // Рассылка всем соединениям строки value
    private void sendToAllConnections (String value) {
        System.out.println(value); // Печатаем строчку в консоль
/*
        int cnt = connections.size(); // Дабы каждый раз в цикле не считать connections.size()
        for ( int i=0; i < cnt; i++ ) connections.get(i).sendString(value); // Рассылаем строчку всем соединениям
*/
        for ( TCPConnection cnct : connections ) cnct.sendString(value); // Рассылаем строчку всем соединениям

        // for (TCPConnection connection : connections) connection.sendString(value); // Рассылаем строчку всем соединениям
    }
}
