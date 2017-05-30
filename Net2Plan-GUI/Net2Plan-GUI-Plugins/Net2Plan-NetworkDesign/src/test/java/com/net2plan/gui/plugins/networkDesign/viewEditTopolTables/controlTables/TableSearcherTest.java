/*******************************************************************************
 * Copyright (c) 2017 Pablo Pavon Marino and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the 2-clause BSD License 
 * which accompanies this distribution, and is available at
 * https://opensource.org/licenses/BSD-2-Clause
 *
 * Contributors:
 *     Pablo Pavon Marino and others - initial API and implementation
 *******************************************************************************/
package com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables;

import com.net2plan.gui.utils.AdvancedJTable;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.commons.lang.ArrayUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.swing.table.DefaultTableModel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Jorge San Emeterio
 * @date 10/05/17
 */
@RunWith(JUnitParamsRunner.class)
public class TableSearcherTest
{
    private static AdvancedJTable table = new AdvancedJTable();
    private static final Object[][] dataVector = new Object[][]
            {
                    {"A", "M", "Z"},
                    {"B", "O", "Y"},
                    {"C", "O", "Z"}
            };

    @BeforeClass
    public static void setUp()
    {
        AdvancedJTable table = new AdvancedJTable(new DefaultTableModel());
        ((DefaultTableModel) table.getModel()).setDataVector(dataVector, dataVector[0]);
    }

    @Test
    @Parameters({"A", "Z", "O"})
    public void searchForItemTest(String searchItem)
    {
        TableSearcher searcher = new TableSearcher(table);

        final int[] rowIndex = searcher.lookFor(searchItem);

        assertThat(rowIndex).isNotNull();

        for (int i : rowIndex)
            assertTrue(ArrayUtils.contains(dataVector[i], searchItem));
    }
}