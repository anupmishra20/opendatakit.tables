package org.opendatakit.testutils;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.opendatakit.aggregate.odktables.rest.ElementType;
import org.opendatakit.aggregate.odktables.rest.entity.Column;
import org.opendatakit.common.android.data.ColumnDefinition;
import org.opendatakit.common.android.data.PossibleTableViewTypes;
import org.opendatakit.common.android.data.TableViewType;
import org.opendatakit.common.android.data.UserTable;
import org.opendatakit.common.android.utilities.ODKDatabaseUtils;
import org.opendatakit.common.android.utilities.TableUtil;
import org.opendatakit.tables.utils.SQLQueryStruct;
import org.opendatakit.tables.views.webkits.Control;
import org.opendatakit.tables.views.webkits.ControlIf;
import org.opendatakit.tables.views.webkits.TableData;
import org.opendatakit.tables.views.webkits.TableDataIf;

import android.database.sqlite.SQLiteDatabase;
import android.webkit.WebView;

/**
 * Constants for use in tests.
 * @author sudar.sam@gmail.com
 *
 */
public class TestConstants {

  public static boolean FORM_IS_USER_DEFINED = false;
  public static String FORM_ID = "testFormId";
  public static String FORM_VERSION = "testFormVersion";
  public static String ROOT_ELEMENT = "testRootElement";
  public static String ROW_NAME = "testRowName";
  public static String SCREEN_PATH = "?testKey=testValue";

  public static String DEFAULT_SQL_WHERE_CALUSE = "dudeWhereIsMyCar";
  public static String[] DEFAULT_SQL_SELECTION_ARGS =
      new String[] { "one", "two" };
  public static String[] DEFAULT_SQL_GROUP_BY =
      new String[] { "group one", "group two" };
  public static String DEFAULT_SQL_HAVING = "anyOldHaving";
  public static String DEFAULT_SQL_ORDER_BY_ELEMENT_KEY =
      "elementKeyByWhichToOrder";
  public static String DEFAULT_SQL_ORDER_BY_DIRECTION =
      "directionByWhichToOrder";

  public static final String DEFAULT_FRAGMENT_TAG = "testFragmentTag";

  public static final int DEFAULT_FRAGMENT_ID = 12345;

  public static final String DEFAULT_FILE_NAME = "test/File/Name";

  public static final String DEFAULT_ROW_ID = "testRowId";
  
  public static class ElementKeys {
    public static final String STRING_COLUMN = "stringColumn";
    public static final String INT_COLUMN = "intColumn";
    public static final String NUMBER_COLUMN = "numberColumn";
    public static final String GEOPOINT_COLUMN = "geo";
    public static final String LATITUDE_COLUMN = "geo_latitude";
    public static final String LONGITUDE_COLUMN = "geo_longitude";
    public static final String ALTITUDE_COLUMN = "geo_altitude";
    public static final String ACCURACY_COLUMN = "geo_accuracy";
    public static final String MISSING_COLUMN = "missingColumn";
  }

  /**
   * The default app name for tables. Using this rather than the
   * getDefaultAppName method because that dumps the stack trace.
   */
  public static final String TABLES_DEFAULT_APP_NAME = "tables";

  public static final String DEFAULT_TABLE_ID = "testTableId";

  /**
   * Return an unimplemented mock of {@link TableProperties}.
   */
  public static void setSingleTableSchemeMock() {
    ODKDatabaseUtils dbUtilMock = mock(ODKDatabaseUtils.class);
    
    ArrayList<String> tableIds = new ArrayList<String>();
    tableIds.add("aTable");
    when(dbUtilMock.getAllTableIds(any(SQLiteDatabase.class))).thenReturn(tableIds);
    ArrayList<Column> columns = new ArrayList<Column>();
    when(dbUtilMock.getUserDefinedColumns(any(SQLiteDatabase.class), eq("aTable"))).thenReturn(columns);
    ODKDatabaseUtils.set(dbUtilMock);
    
    TableUtil util = mock(TableUtil.class);
    when(util.getLocalizedDisplayName(any(SQLiteDatabase.class), 
        eq("aTable"))).thenReturn("aTable");
    when(util.getDefaultViewType(any(SQLiteDatabase.class), 
        eq("aTable"))).thenReturn(TableViewType.SPREADSHEET);
    TableUtil.set(util);
  }
  
  /**
   * Get a mock {@link ColumnDefinition} object. Returns the element key and
   * column types given. As more mockable parameters are needed, they should be
   * added here.
   * @param elementKey
   * @param columnType
   * @return
   */
  public static ColumnDefinition getColumnDefinitionMock(
      String elementKey,
      ElementType columnType) {
    ColumnDefinition result = mock(ColumnDefinition.class);
    doReturn(elementKey).when(result).getElementKey();
    doReturn(columnType).when(result).getType();
    return result;
  }

  public static SQLQueryStruct getSQLQueryStructMock() {
    SQLQueryStruct mock = mock(SQLQueryStruct.class);
    return mock;
  }

  public static UserTable getUserTableMock() {
    UserTable mock = mock(UserTable.class);
    return mock;
  }

  public static Control getControlMock() {
    Control result = mock(Control.class);
    ControlIf interfaceMock = mock(ControlIf.class);
    doReturn(interfaceMock).when(result)
      .getJavascriptInterfaceWithWeakReference();
    return result;
  }
  
  public static PossibleTableViewTypes getAllValidPossibleTableViewTypes() {
    PossibleTableViewTypes allValid = mock(PossibleTableViewTypes.class);
    doReturn(true).when(allValid).spreadsheetViewIsPossible();
    doReturn(true).when(allValid).listViewIsPossible();
    doReturn(true).when(allValid).mapViewIsPossible();
    doReturn(true).when(allValid).graphViewIsPossible();
    Set<TableViewType> allViewTypes = new HashSet<TableViewType>();
    allViewTypes.add(TableViewType.SPREADSHEET);
    allViewTypes.add(TableViewType.LIST);
    allViewTypes.add(TableViewType.MAP);
    allViewTypes.add(TableViewType.GRAPH);
    doReturn(allViewTypes).when(allValid).getAllPossibleViewTypes();
    return allValid;
  }
  
  /**
   * Returns a map with the element keys in {@link ElementKeys}.
   * @return
   */
  public static Map<String, String> getMapOfElementKeyToValue(
      String rawStringValue,
      String rawIntValue,
      String rawNumberValue) {
    Map<String, String> result = new HashMap<String, String>();
    result.put(ElementKeys.STRING_COLUMN, rawStringValue);
    result.put(ElementKeys.INT_COLUMN, rawIntValue);
    result.put(ElementKeys.NUMBER_COLUMN, rawNumberValue);
    return result;
  }

  /**
   * Returns a map with the element keys in {@link ElementKeys}
   * including the missing one.
   * @return
   */
  public static Map<String, String> getMapOfElementKeyToValueIncludingMissing(
      String rawStringValue,
      String rawIntValue,
      String rawNumberValue) {
    Map<String, String> result = new HashMap<String, String>();
    result.put(ElementKeys.STRING_COLUMN, rawStringValue);
    result.put(ElementKeys.INT_COLUMN, rawIntValue);
    result.put(ElementKeys.NUMBER_COLUMN, rawNumberValue);
    result.put(ElementKeys.MISSING_COLUMN, rawStringValue);
    return result;
  }
  
  public static WebView getWebViewMock() {
    return mock(WebView.class);
  }
  
  public static TableData getTableDataMock() {
    TableData result = mock(TableData.class);
    TableDataIf interfaceMock = mock(TableDataIf.class);
    doReturn(interfaceMock).when(result)
      .getJavascriptInterfaceWithWeakReference();
    return result;
  }
}
