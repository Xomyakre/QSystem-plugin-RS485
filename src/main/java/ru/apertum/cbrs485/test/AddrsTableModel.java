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
public class AddrsTableModel extends AbstractTableModel {

    final private RS485Property property;

    public AddrsTableModel(RS485Property property) {
        this.property = property;
    }

    @Override
    public int getRowCount() {
        return property.getOperators().size();
    }

    @Override
    public int getColumnCount() {
        return 6;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {

        switch (columnIndex) {
            case 0:
                return property.getOperators().get(rowIndex).getPoint();
            case 1:
                return property.getOperators().get(rowIndex).getAddressRS();
            case 2:
                return property.getOperators().get(rowIndex).getComPort();
            case 3:
                return property.getOperators().get(rowIndex).getProtocol();
            case 4:
                return property.getOperators().get(rowIndex).getPosition();
            case 5:
                return property.getOperators().get(rowIndex).getDirection();
            default:
                throw new AssertionError();
        }

    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        switch (columnIndex) {
            case 0:
                property.getOperators().get(rowIndex).setPoint((String) aValue);
                break;
            case 1:
                property.getOperators().get(rowIndex).setAddressRS((Integer) aValue);
                break;
            case 2:
                property.getOperators().get(rowIndex).setComPort((String) aValue);
                break;
            case 3:
                property.getOperators().get(rowIndex).setProtocol((Integer) aValue);
                break;
            case 4:
                property.getOperators().get(rowIndex).setPosition((Integer) aValue);
                break;
            case 5:
                property.getOperators().get(rowIndex).setDirection((String) aValue);
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
                return String.class;
            case 3:
                return Integer.class;
            case 4:
                return Integer.class;
            case 5:
                return String.class;
            default:
                throw new AssertionError();
        }
    }

    @Override
    public String getColumnName(int column) {

        switch (column) {
            case 0:
                return "Point";
            case 1:
                return "RS";
            case 2:
                return "COM";
            case 3:
                return "Protocol";
            case 4:
                return "Pos";
            case 5:
                return "Dir";
            default:
                throw new AssertionError();
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return true;
    }
}
