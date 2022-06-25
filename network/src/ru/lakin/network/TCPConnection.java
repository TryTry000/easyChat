package ru.lakin.network;

import java.io.*;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

// Класс TCPConnection - TCP-соединение с некоторым слушателем событий по некоторому сокету
// Нитебезопасный метод. Обращаться можно из любых нитей. То есть, методы надо синхронизировать.
public class TCPConnection {

    private final Socket socket;    // Сокет данного TCP-соединения
    private final Thread rxThread;  // На каждом клиенте - нить данного TCP-соединения, которая будет постоянно
                                    // читать поток ввода ( слушать входящие сообщения ). И, если строчка прилетела,
                                    // будет генерить событие ( событийная система )

    // Если серверу пришло сообщение, он его должен разослать клиентам.
    // Если клиенту пришло сообщение, он его должен вывести в своё окошко.
    // Дабы объекты классов "сервер" и "клиент", слушая входной поток, могли пользоваться одним и тем же
    // классом TCPConnection, применим написанный нами интерфейс TCPConnectionListener
    private final TCPConnectionListener eventListener;
    private final BufferedReader in;  // Буферизированный поток ввода, работающий со строками
    private final BufferedWriter out; // Буферизированный поток вывода, работающий со строками

// ------- КОНСТРУКТОРЫ ---------------------
    // --- Конструктор 2. Создаёт сокет (соединение) по ip-адресу и номеру порта
    public TCPConnection (TCPConnectionListener eventListener, String ipAddr, int port) throws IOException {
        this(eventListener, new Socket(ipAddr, port)); // Из Конструктора 2 вызываем Конструктор 1
    }
    // --- Конструктор 1. Принимает готовый сокет, созданный снаружи.
    // На входе - готовый объект "сокет", интерфейс слушателя событий.
    // Класс - TCP-соединение по этому сокету с этим интерфейсом.
    public TCPConnection (TCPConnectionListener eventListener, Socket socket) throws IOException {
        this.eventListener = eventListener;
        this.socket = socket;
        // На сокете читаем входящий и пишем исходящий потоки - строки в заданной кодировке
        // Берем простой поток ввода|вывода.
        // На его основе разбираем кодировку [Input|Output]Stream[Reader|Writer]
        // Результат оборачиваем в буферизацию
        in = new BufferedReader( new InputStreamReader( socket.getInputStream(),
                                 Charset.forName("UTF-8")));
//                                 Charset.forName(String.valueOf(StandardCharsets.UTF_8)))); //правка!!!
        out = new BufferedWriter( new OutputStreamWriter( socket.getOutputStream(),
                                 Charset.forName("UTF-8")));
//                                 Charset.forName(String.valueOf(StandardCharsets.UTF_8)))); //правка!!!

        // --- СЛУШАЕМ И ПРИНИМАЕМ СООБЩЕНИЕ
        // Создаём нить, которая будет слушать вход TCP-соединение
        // передать ему экземпляр класса, кторый реализует интерфейс Runnable
        // Создаём через анонимный класс: прямо здесь описываем класс Thread(),
        //                                который реализует интерфейс Runnable(): Thread( new Runnable() {}); ,
        //                                оверрайдим у него метод run
        //                                и создаём его экземпляр rxThread = new Thread ...
        rxThread = new Thread ( new Runnable() {
            @Override
            public void run() {
                try {
                    // после запуска нити, слушаем вход сокета
                    // Тут this относилось бы к анонимному классу, который реализует интерфейс, а зачем нам такой сюр?
                    // Нам нужен экземпляр обрамляющего класса TCPConnection - собственно, соединение. Поэтому вот так:
                    eventListener.onConnectionReady(TCPConnection.this);
                    while ( !rxThread.isInterrupted() ) { // Пока нить не прервана, стандартный механизм Java
                        // String msg = in.readLine();
                        // передаём туда объект нашего соединения и саму строчку
                        // eventListener.onReceiveString(TCPConnection.this, msg);
                        eventListener.onReceiveString(TCPConnection.this, in.readLine());
                    }
                } catch ( IOException e) {
                    eventListener.onException(TCPConnection.this, e); // Коль не сложилось, пусть разбирается eventListener
                } finally {
                    eventListener.onDisconnect(TCPConnection.this); // Оповещаем eventListener о разрыве соединения

                }
            }
        });
        rxThread.start(); // Запустили нить, слушающую входящее соединение
    }

    // --- ОТПРАВЛЯЕМ СООБЩЕНИЕ
    public synchronized void sendString ( String value ) {
        try {
            out.write(value + "\r\n"); // Выдаёт строку в буфер с приписанным концом строки
            out.flush(); // Выдаёт буфер в сокет
        } catch (IOException e) {
            eventListener.onException(TCPConnection.this, e); // Коль не сложилось, пусть разбирается eventListener
            disconnect(); // Раз не смогли записать, рассоединяемся
        }
    }

    // --- ОБРЫВАЕМ СОЕДИНЕНИЕ
    public synchronized void disconnect () {
        rxThread.interrupt();
        try { socket.close(); }
        catch (IOException e) {
            eventListener.onException(TCPConnection.this, e); // Коль не сложилось, пусть разбирается eventListener
        }
    }
    // Переопределим метод toString для вывода логов
    @Override
    public String toString() {
        return ("TCPConnection: " + socket.getInetAddress() + ": " + socket.getPort());
    }
}
