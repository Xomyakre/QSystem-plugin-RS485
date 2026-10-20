/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.apertum.cbrs485.test;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import javax.swing.table.AbstractTableModel;
import javax.xml.bind.JAXBException;
import ru.apertum.cbrs485.core.RS485Property;

/**
 *
 * @author egorov
 */
public class PortsTableModel extends AbstractTableModel {

    final private RS485Property property;

    public PortsTableModel(RS485Property property) {
        this.property = property;
    }

    @Override
    public int getRowCount() {
        return property.getPorts().size();
    }

    @Override
    public int getColumnCount() {
        return 5;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {

        switch (columnIndex) {
            case 0:
                return property.getPorts().get(rowIndex).getPortName();
            case 1:
                return property.getPorts().get(rowIndex).getSpeed();
            case 2:
                return property.getPorts().get(rowIndex).getBits();
            case 3:
                return property.getPorts().get(rowIndex).getStopBits();
            case 4:
                return property.getPorts().get(rowIndex).getParity();
            default:
                throw new AssertionError();
        }

    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        switch (columnIndex) {
            case 0:
                property.getPorts().get(rowIndex).setPortName((String) aValue);
                break;
            case 1:
                property.getPorts().get(rowIndex).setSpeed((Integer) aValue);
                break;
            case 2:
                property.getPorts().get(rowIndex).setBits((Integer) aValue);
                break;
            case 3:
                property.getPorts().get(rowIndex).setStopBits((Integer) aValue);
                break;
            case 4:
                property.getPorts().get(rowIndex).setParity((Boolean) aValue);
                break;
            default:
                throw new AssertionError();
        }

        try {
            property.marshal(new FileOutputStream(RS485Property.PROP_FILE));
        } catch (FileNotFoundException ex) {
            throw new RuntimeException("Не найден файл сохранения настроек." + ex.toString());
        } catch (JAXBException ex) {
            throw new RuntimeException("Загнулся маршалинг. Изменение расстановой не пошло на пользу. " + ex.toString());
        }

    }

    @Override
    public void fireTableDataChanged() {
        super.fireTableDataChanged();
        try {
            property.marshal(new FileOutputStream(RS485Property.PROP_FILE));
        } catch (FileNotFoundException ex) {
            throw new RuntimeException("Не найден файл сохранения настроек." + ex.toString());
        } catch (JAXBException ex) {
            throw new RuntimeException("Загнулся маршалинг. Изменение расстановой не пошло на пользу. " + ex.toString());
        }
    }

    @Override
    public void fireTableStructureChanged() {
        super.fireTableStructureChanged();
        try {
            property.marshal(new FileOutputStream(RS485Property.PROP_FILE));
        } catch (FileNotFoundException ex) {
            throw new RuntimeException("Не найден файл сохранения настроек." + ex.toString());
        } catch (JAXBException ex) {
            throw new RuntimeException("Загнулся маршалинг. Изменение расстановой не пошло на пользу. " + ex.toString());
        }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case 0:
                return String.class;
            case 1:
                return Integer.class;
            case 2:
                return Integer.class;
            case 3:
                return Integer.class;
            case 4:
                return Boolean.class;
            default:
                throw new AssertionError();
        }
    }

    @Override
    public String getColumnName(int column) {

        switch (column) {
            case 0:
                return "PortName";
            case 1:
                return "Speed";
            case 2:
                return "Bits";
            case 3:
                return "StopBits";
            case 4:
                return "Parity";
            default:
                throw new AssertionError();
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return true;
    }
}
