package com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables;

import com.net2plan.gui.utils.AdvancedJTable;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.commons.lang.ArrayUtils;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.swing.table.DefaultTableModel;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Jorge San Emeterio
 * @date 10/05/17
 */
@RunWith(JUnitParamsRunner.class)
public class TableSearcherTest
{
    @Test
    @Parameters({"A", "Z", "O"})
    public void searchForItemTest(String searchItem)
    {
        AdvancedJTable table = new AdvancedJTable();
        table.setModel(new DefaultTableModel());

        final Object[][] dataVector = new Object[][]
                {
                        {"A", "M", "Z"},
                        {"B", "O", "Y"},
                        {"C", "O", "Z"}
                };
        ((DefaultTableModel) table.getModel()).setDataVector(dataVector, dataVector[0]);

        TableSearcher searcher = new TableSearcher(table);
        final int[] rowIndex = searcher.lookFor(searchItem);

        assertNotNull(rowIndex);

        for (int i : rowIndex)
            assertTrue(ArrayUtils.contains(dataVector[i], searchItem));
    }
}