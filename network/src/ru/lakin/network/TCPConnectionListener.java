package ru.lakin.network;

// Если серверу пришло сообщение, он его должен разослать клиентам.
// Если клиенту пришло сообщение, он его должен вывести в своё окошко.
// Дабы объекты классов "сервер" и "клиент", слушая входной поток, могли пользоваться одним и тем же
// классом TCPConnection, напишем интерфейс TCPConnectionListener:
public interface TCPConnectionListener {
    // Чтобы у класса, реализующего интерфейс, был доступ к событиям:
    void onConnectionReady (TCPConnection tcpConnection);              // Соединение запустилось, можно работать
    void onReceiveString (TCPConnection tcpConnection, String value);  // Приняли входящую строку
    void onDisconnect (TCPConnection tcpConnection);                   // Соединение разорвалось
    void onException (TCPConnection tcpConnection, Exception e);       // Ошибка соединения
}
