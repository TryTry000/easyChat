package ru.lakin.chat.client;
import javax.swing.*; // Выкатывать гаубицу JavaFX не будем, обойдёмся окошком от Swing
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import ru.lakin.network.*;
import static ru.lakin.network.NetworkEnv.IP_ADDR;
import static ru.lakin.network.NetworkEnv.PORT;


// В библиотеке Swing описан класс JFrame, представляющий собой окно с рамкой и строкой заголовка со стандартными
// "кнопочками" «Свернуть», «Во весь экран» и «Закрыть». Окно может изменять размеры и перемещаться по экрану.
// Конструкторы класса JFrame:
// --- Конструктор JFrame() без параметров создает пустое окно.
// --- Конструктор JFrame(String title) создает пустое окно с заголовком title.
// Среди методов класса JFrame:
// --- setSize(int width, int height) — устанавливает размеры окна. Если не задать размеры, окно будет иметь нулевую
// высоту независимо от того, что в нем находится. И пользователю после запуска придется растягивать окно вручную.
// Размеры окна включают в себя не только «рабочую» область, но и границы, и строку заголовка.
// --- setDefaultCloseOperation(int operation) — позволяет указать действие, которое необходимо выполнить, когда
// пользователь закрывает окно нажатием на крестик. Обычно в программе есть одно или несколько окон при закрытии
// которых программа прекращает работу. Для того, чтобы запрограммировать это поведение, следует в качестве параметра
// operation передать константу EXIT_ON_CLOSE, описанную в классе JFrame.
// --- setAlwaysOnTop(boolean onTop) — Окно всегда - наверху
// --- setVisible(boolean visible) — когда окно создается, оно по умолчанию невидимо. Чтобы отобразить окно на экране,
// вызывается данный метод с параметром true. Если вызвать его с параметром false, окно снова станет невидимым.

// Интерфейс ActionListener требует реализации метода actionPerformed(ActionEvent e)
// Интерфейс TCPConnectionListener требует реализации методов
//                                          onConnectionReady, onReceiveString, onDisconnect, onException
public class ClientWindow extends JFrame implements ActionListener, TCPConnectionListener {
    // Размеры окна
    private static final int WIDTH = 480;
    private static final int HEIGHT = 320;

    public static void main (String[] args) {
        // Графические интерфейсы, как правило, требуют работы только в одной нити - главной
        // У Свинга - ещё более крутые ограничения, он не работает даже в главной нити. А работает лишь в нити
        // EDT (Event Dispatching Thread) - специальная нить, используемая для обработки событий из очереди событий.
        // Такой подход является концептом событийно-ориентированного программирования.
        //
        // Вызывая метод invokeLater и передавая туда экземпляр анонимного класса, мы как раз заставляем
        // строку new ClientWindow(); выполниться в нитке EDT
/*
        SwingUtilities.invokeLater ( new Runnable() {
            @Override
            public void run () { new ClientWindow(); }
        });
*/      // Так ИДЕА красиво-непонятно переписала закомментаренный выше код
        SwingUtilities.invokeLater (ClientWindow::new);
    }

    private final JTextArea log = new JTextArea(); // Поле чата. Класс библиотеки JTextArea
    private final JTextField fieldNickname = new JTextField("Аноним"); // Простое однострочное поле ввода имени
    private final JTextField fieldInput = new JTextField(); // Простое однострочное поле ввода сообщения

    // Нужно установить сетевое соединение с помощью нашего класса TCPConnection
    private TCPConnection connection;

    // ------- Конструктор ---------------------------
    private ClientWindow() {
        // Закрытие окна по крестику
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(WIDTH, HEIGHT); // Устанавливаем размеры окна
        setLocationRelativeTo(null); // Окно в середину экрана
        setAlwaysOnTop(true); // Окно всегда наверху
        log.setEditable(false); // Не разрешаем редактировать поле чата
        log.setLineWrap(true); // Разрешаем перенос строк в поле чата
        add(log, BorderLayout.CENTER); // Поле чата ставим в центр окна

        // На нижнем поле ждём в нижнем поле ввода и ентера. Вешаем на него ActionListener и передаём ему экземпляр,
        // который реализует интерфейс addActionListener. В данном случае в нашем классе ClientWindow мы реализовали
        // интерфейс addActionListener и передали туда себя.
        fieldInput.addActionListener(this);
        add(fieldInput, BorderLayout.SOUTH); // Поле сообщения вниз
        add(fieldNickname, BorderLayout.NORTH); // Поле имени наверх

        setVisible(true); // Окно видимо
        try { connection = new TCPConnection(this, IP_ADDR, PORT); } // Передаём себя в TCPConnection
        catch (IOException e) { printMsg ( "Ошибка соединения: " + e ); }
    }
    @Override
    // Метод интерфейса ActionListener, куда передаётся событие ActionEvent
    // На текстовом поле ActionEvent происходит когда нажимается Enter.
    // На кнопке ActionEvent происходит когда мышь кликает по кнопке.
    public void actionPerformed(ActionEvent e) {
        // Берем введённую строчку msg
        String msg = fieldInput.getText();
        if ( msg.equals("")) return;
        fieldInput.setText(null);
        connection.sendString(fieldNickname.getText() + ": " + msg);
    }

    // ----- Методы интерфейса TCPConnectionListener. Синхронизировать их не надо, так как они будут вызываться из
    // одной нити.
    @Override
    public void onConnectionReady(TCPConnection tcpConnection) {
        printMsg("Соединение установлено...");
    }
    @Override
    public void onReceiveString(TCPConnection tcpConnection, String value) {
        printMsg(value);
    }
    @Override
    public void onDisconnect(TCPConnection tcpConnection) {
        printMsg("Соединение отвалилось...");
    }
    @Override
    public void onException(TCPConnection tcpConnection, Exception e) {
        printMsg ( "Ошибка соединения: " + e );
    }

    // Метод будет писать в наше текстовое поле. Будем писать из нити нашего окошка и из нити соединения,
    // поэтому синхронизируем. И снова шаманство для запуска Swing.
    private synchronized void printMsg(String msg) {
/*
        SwingUtilities.invokeLater ( new Runnable() {
            @Override
            public void run() {
                log.append(msg + "\n");
                // Гарантированное срабатывание скролла: ставим каретку в конец
                log.setCaretPosition(log.getDocument().getLength());
            }
        });
*/      // Так красиво-непонятно переписала ИДЕА закоментаренный ВЫШЕ код
        SwingUtilities.invokeLater (() -> {
            log.append(msg + "\n");
            // Гарантированное срабатывание скролла: ставим каретку в конец
            log.setCaretPosition(log.getDocument().getLength());
        });
    }
}

