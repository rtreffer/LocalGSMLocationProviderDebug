package org.gfd.gsmlocation;

import org.gfd.gsmlocation.model.CellInfo;

interface BackendDebug {
    CellInfo[] getUsedCells();
    CellInfo[] getUnusedCells();
}
