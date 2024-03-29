/*
 * Copyright (C) 2012-2014 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.opendatakit.tables.utils;

import org.opendatakit.common.android.data.ColorRuleGroup;
import org.opendatakit.tables.activities.TableDisplayActivity.ViewFragmentType;
import org.opendatakit.tables.activities.TableLevelPreferencesActivity;
import org.opendatakit.tables.utils.Constants.IntentKeys;

import android.os.Bundle;

/**
 *
 * @author sudar.sam@gmail.com
 *
 */
public class IntentUtil {

  /**
   * Retrieve the file name from the saved instance state or from the
   * other bundle. Convenience method for calling
   * {@link #retrieveAppNameFromBundle(Bundle)} in the appropriate order,
   * respecting that savedInstanceState may be null.
   * <p>
   * If the file name is non-null in both bundles, savedInstanceState takes
   * precedent.
   * @param savedInstanceState
   * @param argumentsOrIntentExtras the bundle from either the activity's
   * starting intent or the fragment's arguments.
   * @return the file name, or null if the value does not exist in either
   * bundle.
   */
  public static String retrieveFileNameFromSavedStateOrArguments(
      Bundle savedInstanceState,
      Bundle argumentsOrIntentExtras) {
    String result = null;
    if (savedInstanceState != null) {
      result = retrieveFileNameFromBundle(savedInstanceState);
    }
    if (result == null) {
      result = retrieveFileNameFromBundle(argumentsOrIntentExtras);
    }
    return result;
  }

  /**
   * Retrieve a {@link SQLQueryStruct} from bundle. The various components
   * should be keyed to the SQL intent keys in {@link Constants.IntentKeys}.
   * @param bundle
   * @return
   */
  public static SQLQueryStruct getSQLQueryStructFromBundle(Bundle bundle) {
    String sqlWhereClause =
        bundle.getString(IntentKeys.SQL_WHERE);
    String[] sqlSelectionArgs = null;
    if (sqlWhereClause != null && sqlWhereClause.length() != 0) {
       sqlSelectionArgs = bundle.getStringArray(
          IntentKeys.SQL_SELECTION_ARGS);
    }
    String[] sqlGroupBy = bundle.getStringArray(IntentKeys.SQL_GROUP_BY_ARGS);
    String sqlHaving = null;
    if ( sqlGroupBy != null && sqlGroupBy.length != 0 ) {
      sqlHaving = bundle.getString(IntentKeys.SQL_HAVING);
    }
    String sqlOrderByElementKey = bundle.getString(
        IntentKeys.SQL_ORDER_BY_ELEMENT_KEY);
    String sqlOrderByDirection = null;
    if ( sqlOrderByElementKey != null && sqlOrderByElementKey.length() != 0 ) {
      sqlOrderByDirection = bundle.getString(
          IntentKeys.SQL_ORDER_BY_DIRECTION);
      if ( sqlOrderByDirection == null || sqlOrderByDirection.length() == 0 ) {
        sqlOrderByDirection = "ASC";
      }
    }
    SQLQueryStruct result = new SQLQueryStruct(
        sqlWhereClause,
        sqlSelectionArgs,
        sqlGroupBy,
        sqlHaving,
        sqlOrderByElementKey,
        sqlOrderByDirection);
    return result;
  }

  /**
   * Return the file name from the bundle. Convenience method for calling
   * {@link Bundle#getString(String)} with
   * {@link Constants.IntentKeys#FILE_NAME}.
   * @param bundle
   * @return the file name, null if it does not exist or if bundle is null
   */
  public static String retrieveFileNameFromBundle(Bundle bundle) {
    if (bundle == null) {
      return null;
    }
    String fileName = bundle.getString(Constants.IntentKeys.FILE_NAME);
    return fileName;
  }
  
  /**
   * Return the {@link ColorRuleGroup.Type} from the bundle. Convenience method
   * for calling {@link Bundle#get(String)} with
   * {@link Constants.IntentKeys#COLOR_RULE_TYPE} and parsing the resultant
   * value.
   * @param bundle
   * @return
   */
  public static ColorRuleGroup.Type retrieveColorRuleTypeFromBundle(
      Bundle bundle) {
    if (bundle == null) {
      return null;
    }
    String typeStr = bundle.getString(Constants.IntentKeys.COLOR_RULE_TYPE);
    if (typeStr == null) {
      return null;
    } else {
      return ColorRuleGroup.Type.valueOf(typeStr);
    }
  }

  /**
   * Return the table id from the bundle. Convenience method for calling
   * {@link Bundle#getString(String)} with
   * {@link Constants.IntentKeys#TABLE_ID}.
   * @param bundle
   * @return the table id, null if it does not exist or if bundle is null
   */
  public static String retrieveTableIdFromBundle(Bundle bundle) {
    if (bundle == null) {
      return null;
    }
    String tableId = bundle.getString(IntentKeys.TABLE_ID);
    return tableId;
  }

  /**
   * Return the app name from the bundle. Convenience method for calling
   * {@link Bundle#getString(String)} with
   * {@link Constants.IntentKeys#APP_NAME}.
   * @param bundle
   * @return the app name, null if it does not exist or if bundle is null
   */
  public static String retrieveAppNameFromBundle(Bundle bundle) {
    if (bundle == null) {
      return null;
    }
    String appName = bundle.getString(IntentKeys.APP_NAME);
    return appName;
  }
  
  /**
   * Return the element key from the bundle. Convenience method for calling
   * {@link Bundle#getString(String)} with
   * {@link Constants.IntentKeys#ELEMENT_KEY}.
   * @param bundle
   * @return the element key, null if it does not exist or if the bundle is
   * null
   */
  public static String retrieveElementKeyFromBundle(Bundle bundle) {
    if (bundle == null) { 
      return null;
    }
    String elementKey = bundle.getString(IntentKeys.ELEMENT_KEY);
    return elementKey;
  }

  /**
   * Return the row id from the bundle. Convenience method for calling
   * {@link Bundle#getString(String)} with
   * {@link Constants.IntentKeys#ROW_ID}.
   * @param bundle
   * @return the row id, null if it does not exist or if bundle is null
   */
  public static String retrieveRowIdFromBundle(Bundle bundle) {
    if (bundle == null) {
      return null;
    }
    String rowId = bundle.getString(IntentKeys.ROW_ID);
    return rowId;
  }

  /**
   * Add values to intent to prepare to launch a detail view. Convenience
   * method for calling the corresponding methods in this class, including
   * {@link #addFragmentViewTypeToBundle(Bundle, ViewFragmentType)} with
   * {@link ViewFragmentType#DETAIL}.
   * @param intent
   * @param appName
   * @param tableId
   * @param rowId
   * @param fileName
   */
  public static void addDetailViewKeysToIntent(
      Bundle bundle,
      String appName,
      String tableId,
      String rowId,
      String fileName) {
    addAppNameToBundle(bundle, appName);
    addTableIdToBundle(bundle, tableId);
    addRowIdToBundle(bundle, rowId);
    addFileNameToBundle(bundle, fileName);
    addFragmentViewTypeToBundle(bundle, ViewFragmentType.DETAIL);
  }

  /**
   * Add viewFragmentType's {@link ViewFragmentType#name()} to bundle. If
   * bundle or viewFragmentType is null, does nothing.
   * @param bundle
   * @param viewFragmentType
   */
  public static void addFragmentViewTypeToBundle(
      Bundle bundle,
      ViewFragmentType viewFragmentType) {
    if (bundle != null && viewFragmentType != null) {
      bundle.putString(
          IntentKeys.TABLE_DISPLAY_VIEW_TYPE,
          viewFragmentType.name());
    }
  }

  /**
   * Add the sql keys to the bundle. Convenience method for calling the
   * corresponding add methods in this class.
   * @param bundle
   * @param whereClause
   * @param selectionArgs
   * @param groupBy
   * @param having
   * @param orderByElementKey
   * @param orderByDirection
   */
  public static void addSQLKeysToBundle(
      Bundle bundle,
      String whereClause,
      String[] selectionArgs,
      String[] groupBy,
      String having,
      String orderByElementKey,
      String orderByDirection) {
    addWhereClauseToBundle(bundle, whereClause);
    addSelectionArgsToBundle(bundle, selectionArgs);
    addGroupByToBundle(bundle, groupBy);
    addHavingToBundle(bundle, having);
    addOrderByElementKeyToBundle(bundle, orderByElementKey);
    addOrderByDirectionToBundle(bundle, orderByDirection);
  }

  /**
   * Add orderByElementKey to bundle keyed to
   *  {@link IntentKeys#SQL_ORDER_BY_ELEMENT_KEY}.
   * If bundle or orderByElementKey is null, does nothing.
   * @param bundle
   * @param whereClause
   */
  public static void addOrderByElementKeyToBundle(
      Bundle bundle,
      String orderByElementKey) {
    if (bundle != null && orderByElementKey != null) {
      bundle.putString(IntentKeys.SQL_ORDER_BY_ELEMENT_KEY, orderByElementKey);
    }
  }

  /**
   * Add orderByDirection to bundle keyed to
   *  {@link IntentKeys#SQL_ORDER_BY_DIRECTION}.
   * If bundle or orderByDirection is null, does nothing.
   * @param bundle
   * @param orderByDirection
   */
  public static void addOrderByDirectionToBundle(
      Bundle bundle,
      String orderByDirection) {
    if (bundle != null && orderByDirection != null) {
      bundle.putString(IntentKeys.SQL_ORDER_BY_DIRECTION, orderByDirection);
    }
  }

  /**
   * Add whereClause to bundle keyed to {@link IntentKeys#SQL_WHERE}.
   * If bundle or whereClause is null, does nothing.
   * @param bundle
   * @param whereClause
   */
  public static void addWhereClauseToBundle(
      Bundle bundle,
      String whereClause) {
    if (bundle != null && whereClause != null) {
      bundle.putString(IntentKeys.SQL_WHERE, whereClause);
    }
  }

  /**
   * Add selectionArgs to bundle keyed to
   * {@link IntentKeys#SQL_SELECTION_ARGS}.
   * If bundle or selectionArgs is null, does nothing.
   * @param bundle
   * @param selectionArgs
   */
  public static void addSelectionArgsToBundle(
      Bundle bundle,
      String[] selectionArgs) {
    if (bundle != null && selectionArgs != null) {
      bundle.putStringArray(IntentKeys.SQL_SELECTION_ARGS, selectionArgs);
    }
  }

  /**
   * Add having to bundle keyed to
   * {@link IntentKeys#SQL_HAVING}.
   * If bundle or having is null, does nothing.
   * @param bundle
   * @param having
   */
  public static void addHavingToBundle(Bundle bundle, String having) {
    if (bundle != null && having != null) {
      bundle.putString(IntentKeys.SQL_HAVING, having);
    }
  }

  /**
   * Add groupBy to bundle keyed to
   * {@link IntentKeys#SQL_GROUP_BY_ARGS}.
   * If bundle or groupBy is null, does nothing.
   * @param bundle
   * @param groupBy
   */
  public static void addGroupByToBundle(
      Bundle bundle,
      String[] groupBy) {
    if (bundle != null && groupBy != null) {
      bundle.putStringArray(IntentKeys.SQL_GROUP_BY_ARGS, groupBy);
    }
  }

  /**
   * Add appName to the bundle keyed to {@link Constants.IntentKeys#APP_NAME}.
   * If bundle or appName is null, does nothing.
   * @param bundle
   * @param appName
   */
  public static void addAppNameToBundle(Bundle bundle, String appName) {
    if (bundle != null && appName != null) {
      bundle.putString(Constants.IntentKeys.APP_NAME, appName);
    }
  }
  
  /**
   * Add tye name of type to the bundle keyed to
   * {@link Constants.IntentKeys#COLOR_RULE_TYPE}. If bundle or type is null,
   * does nothing.
   * @param bundle
   * @param type
   */
  public static void addColorRuleGroupTypeToBundle(
      Bundle bundle,
      ColorRuleGroup.Type type) {
    if (bundle != null && type != null) {
      bundle.putString(Constants.IntentKeys.COLOR_RULE_TYPE, type.name());
    }
  }
  
  /**
   * Add elementKey to bundle keyed to
   * {@link Constants.IntentKeys#ELEMENT_KEY}. If bundle of elementKey is null,
   * does nothing.
   * @param bundle
   * @param elementKey
   */
  public static void addElementKeyToBundle(Bundle bundle, String elementKey) {
    if (bundle != null && elementKey != null) {
      bundle.putString(Constants.IntentKeys.ELEMENT_KEY, elementKey);
    }
  }

  /**
   * Add tableId to bundle keyed to {@link Constants.IntentKeys#TABLE_ID}.
   * If bundle or appName is null, does nothing.
   * @param bundle
   * @param tableId
   */
  public static void addTableIdToBundle(Bundle bundle, String tableId) {
    if (bundle != null && tableId != null) {
      bundle.putString(Constants.IntentKeys.TABLE_ID, tableId);
    }
  }
  
  /**
   * Add the name of fragmentType to bundle, keyed to
   * {@link Constants.IntentKeys#TABLE_PREFERENCE_FRAGMENT_TYPE}. If bundle
   * or fragmentType is null, does nothing.
   * @param bundle
   * @param fragmentType
   */
  public static void addTablePreferenceFragmentTypeToBundle(
      Bundle bundle,
      TableLevelPreferencesActivity.FragmentType fragmentType) {
    if (bundle != null && fragmentType != null) {
      bundle.putString(
          Constants.IntentKeys.TABLE_PREFERENCE_FRAGMENT_TYPE,
          fragmentType.name());
    }
  }

  /**
   * Add rowId to bundle keyed to {@link Constants.IntentKeys#ROW_ID}.
   * If bundle or rowId is null, does nothing.
   * @param bundle
   * @param rowId
   */
  public static void addRowIdToBundle(Bundle bundle, String rowId) {
    if (bundle != null && rowId != null) {
      bundle.putString(Constants.IntentKeys.ROW_ID, rowId);
    }
  }

  /**
   * Add fileName to bundle keyed to {@link Constants.IntentKeys#FILE_NAME}.
   * If bundle or fileName is null, does nothing.
   * @param bundle
   * @param fileName
   */
  public static void addFileNameToBundle(Bundle bundle, String fileName) {
    if (bundle != null && fileName != null) {
      bundle.putString(Constants.IntentKeys.FILE_NAME, fileName);
    }
  }

}
