package ru.apertum.cbrs485.test;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import ru.apertum.cbrs485.core.Event;
import ru.apertum.cbrs485.core.RS485Property;
import ru.apertum.cbrs485.core.RS485Property.Operator;
import ru.apertum.qsystem.common.CustomerState;
import ru.evgenic.rxtx.serialPort.ISerialPort;

/**
 * Прогоняет тот же путь, что и плагин в QSystem: событие -> протокол оператора -> байты -> COM порт.
 * Вызов: номер мигает. Начало приема: номер горит. Завершение: табло гаснет.
 */
public class Pr9800ScenarioPanel extends JPanel {

    public interface PortOpener {

        ISerialPort open() throws Exception;
    }

    private final Supplier<RS485Property> props;
    private final PortOpener portOpener;
    private final JComboBox<String> cbPoint = new JComboBox<>();
    private final JTextField tfTicket = new JTextField("А001", 8);
    private final JSpinner spDelay = new JSpinner(new SpinnerNumberModel(5, 1, 60, 1));
    private final JTextArea taLog = new JTextArea(15, 60);
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        final Thread t = new Thread(r, "pr9800-scenario");
        t.setDaemon(true);
        return t;
    });

    public Pr9800ScenarioPanel(Supplier<RS485Property> props, PortOpener portOpener) {
        super(new BorderLayout());
        this.props = props;
        this.portOpener = portOpener;

        final JButton btnRefresh = new JButton("Обновить окна");
        final JButton btnInvite = new JButton("1. Вызвать");
        final JButton btnWork = new JButton("2. Начать прием");
        final JButton btnFinish = new JButton("3. Завершить");
        final JButton btnAll = new JButton("Весь сценарий");

        btnRefresh.addActionListener(e -> refreshPoints());
        btnInvite.addActionListener(e -> worker.submit(() -> send(CustomerState.STATE_INVITED)));
        btnWork.addActionListener(e -> worker.submit(() -> send(CustomerState.STATE_WORK)));
        btnFinish.addActionListener(e -> worker.submit(() -> send(CustomerState.STATE_FINISH)));
        btnAll.addActionListener(e -> worker.submit(this::runScenario));

        final JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Окно:"));
        top.add(cbPoint);
        top.add(btnRefresh);
        top.add(new JLabel("Талон:"));
        top.add(tfTicket);
        top.add(btnInvite);
        top.add(btnWork);
        top.add(btnFinish);
        top.add(btnAll);
        top.add(new JLabel("пауза, с:"));
        top.add(spDelay);

        taLog.setEditable(false);
        taLog.setFont(new Font("Monospaced", Font.PLAIN, 12));
        add(top, BorderLayout.NORTH);
        add(new JScrollPane(taLog), BorderLayout.CENTER);

        refreshPoints();
    }

    private void refreshPoints() {
        cbPoint.removeAllItems();
        final RS485Property rsp = props.get();
        if (rsp == null) {
            return;
        }
        for (Operator o : rsp.getOperators()) {
            cbPoint.addItem(o.getPoint());
        }
    }

    private void runScenario() {
        final long delay = ((Integer) spDelay.getValue()) * 1000L;
        try {
            send(CustomerState.STATE_INVITED);
            Thread.sleep(delay);
            send(CustomerState.STATE_WORK);
            Thread.sleep(delay);
            send(CustomerState.STATE_FINISH);
        } catch (InterruptedException ex) {
            log("Сценарий прерван");
        }
    }

    private void send(CustomerState state) {
        final RS485Property rsp = props.get();
        if (rsp == null) {
            log("Нет настроек: " + RS485Property.PROP_FILE + " не загружен");
            return;
        }
        final String point = (String) cbPoint.getSelectedItem();
        final Operator op = point == null ? null : rsp.getOperatorByPoint(point);
        if (op == null) {
            log("Окно не выбрано или его нет на вкладке \"Оператор<->Табло\"");
            return;
        }
        final String ticket = tfTicket.getText().trim();
        final boolean clear = state == CustomerState.STATE_FINISH;
        final Event event = new Event(point, clear ? "     " : ticket, "", 0, state);
        final RS485Property.Protocol protocol = op.getNProtocol(rsp);

        log("--> " + state + "  окно=" + point + " адрес=" + op.getAddressRS() + " протокол=" + protocol);
        final byte[] bytes = protocol.getData(event, op, rsp);
        if (bytes == null) {
            log("    протокол ничего не вернул для " + state);
            return;
        }
        final StringBuilder hex = new StringBuilder("    HEX: ");
        for (byte b : bytes) {
            hex.append(String.format("%02X ", b & 0xFF));
        }
        log(hex.toString());

        ISerialPort port = null;
        try {
            port = portOpener.open();
            port.send(bytes);
            Thread.sleep(250);
            log("    отправлено");
        } catch (Exception ex) {
            log("    ОШИБКА: " + ex);
            System.err.println(ex);
        } finally {
            if (port != null) {
                try {
                    port.free();
                } catch (Exception ex) {
                    System.err.println(ex);
                }
            }
        }
    }

    private void log(String s) {
        System.out.println(s);
        SwingUtilities.invokeLater(() -> taLog.append(s + "\n"));
    }
}
