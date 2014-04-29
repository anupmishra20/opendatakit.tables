/*
 * Copyright (C) 2012 University of Washington
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
package org.opendatakit.tables.activities;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.opendatakit.common.android.data.ColumnProperties;
import org.opendatakit.common.android.data.DbTable;
import org.opendatakit.common.android.data.KeyValueStoreHelper;
import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.common.android.data.TableViewType;
import org.opendatakit.common.android.data.UserTable;
import org.opendatakit.common.android.utils.DataUtil;
import org.opendatakit.tables.R;
import org.opendatakit.tables.types.FormType;
import org.opendatakit.tables.utils.CollectUtil;
import org.opendatakit.tables.utils.TableFileUtils;
import org.opendatakit.tables.utils.CollectUtil.CollectFormParameters;
import org.opendatakit.tables.utils.SurveyUtil;
import org.opendatakit.tables.utils.SurveyUtil.SurveyFormParameters;
import org.opendatakit.tables.views.CellValueView;
import org.opendatakit.tables.views.ClearableEditText;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * A controller for the elements common to the various table display activities.
 *
 * The general weirdness of how this package is structured (i.e., a Controller
 * class used by unrelated display activities, instead of just having those
 * display activities subclass a common parent) is because the Google Maps API
 * requires that activities that use MapViews extend the Android MapActivity
 * (meaning that the MapDisplayActivity couldn't extend the common display
 * activity unless the common display activity extended the Android MapActivity
 * class, which seemed undesirable since that would require that all of the
 * display activities be children of MapActivity for no good reason).
 */
public class Controller {

  private static final String TAG = "Controller";

  public static final String INTENT_KEY_APP_NAME = "appName";
  public static final String INTENT_KEY_TABLE_ID = "tableId";
 // public static final String INTENT_KEY_SEARCH_STACK = "searchStack";
  public static final String INTENT_KEY_IS_OVERVIEW = "isOverview";
  /**
   * Key to the where clause if this list view is to be opened with a more
   * complex query than permissible by the simple query object. Must conform
   * to the expectations of {@link DbTable#rawSqlQuery} and
   * {@link CustomView$Control#queryWithSql}.
   *
   * @see INTENT_KEY_SQL_SELECTION_ARGS
   */
  public static final String INTENT_KEY_SQL_WHERE = "sqlWhereClause";
  /**
   * An array of strings for restricting the rows displayed in the table.
   *
   * @see INTENT_KEY_SQL_WHERE
   */
  public static final String INTENT_KEY_SQL_SELECTION_ARGS = "sqlSelectionArgs";
  /**
   * An array of strings giving the group by columns.
   * What was formerly 'overview' mode is a non-null groupBy list.
   */
  public static final String INTENT_KEY_SQL_GROUP_BY_ARGS = "sqlGroupByArgs";
  /**
   * The having clause, if present
   */
  public static final String INTENT_KEY_SQL_HAVING = "sqlHavingClause";
  /**
   * The order by column. NOTE: restricted to a single column
   */
  public static final String INTENT_KEY_SQL_ORDER_BY_ELEMENT_KEY = "sqlOrderByElementKey";
  /**
   * The order by direction (ASC or DESC)
   */
  public static final String INTENT_KEY_SQL_ORDER_BY_DIRECTION = "sqlOrderByDirection";

  public static final String INTENT_KEY_CURRENT_VIEW_TYPE = "currentViewType";

  public static final int VIEW_ID_SEARCH_FIELD = 0;
  public static final int VIEW_ID_SEARCH_BUTTON = 1;

  private static final int MENU_ITEM_ID_SEARCH_BUTTON = 0;
  private static final int MENU_ITEM_ID_VIEW_TYPE_SUBMENU = 1;
  // The add row button serves as an edit row button in DetailDisplayActivity
  public static final int MENU_ITEM_ID_ADD_ROW_BUTTON = 2;
  private static final int MENU_ITEM_ID_SETTINGS_SUBMENU = 3;
  // Display preferences is used differently in SpreadsheetDisplayActivity
  public static final int MENU_ITEM_ID_DISPLAY_PREFERENCES = 4;
  private static final int MENU_ITEM_ID_OPEN_TABLE_PROPERTIES = 5;
  private static final int MENU_ITEM_ID_OPEN_COLUMN_MANAGER = 6;
  private static final int MENU_ITEM_ID_OPEN_LIST_VIEW_MANAGER = 7;
  static final int FIRST_FREE_MENU_ITEM_ID = 8;

  public static final int RCODE_TABLE_PROPERTIES_MANAGER = 0;
  public static final int RCODE_COLUMN_MANAGER = 1;
  public static final int RCODE_ODK_COLLECT_ADD_ROW = 2;
  public static final int RCODE_ODK_COLLECT_EDIT_ROW = 3;
  public static final int RCODE_LIST_VIEW_MANAGER = 4;
  /**
   * This is the return code for when Collect is called to add a row to a
   * table that is not the table held by the activity at the time of the
   * call. E.g. Controller holds a TableProperties for a table that is
   * currently being displayed. If we are in a list view, and the user wants
   * to add a row to a table other than the table currently being displayed
   * in the list view, this is the return code that should be used. The
   * caller also must be sure to launch the intent using
   * {@link CollectUtil#launchCollectToAddRow(Activity, Intent, TableProperties)}
   * .
   */
  public static final int RCODE_ODK_COLLECT_ADD_ROW_SPECIFIED_TABLE = 5;
  public static final int RCODE_ODK_SURVEY_ADD_ROW = 6;
  public static final int RCODE_ODK_SURVEY_EDIT_ROW = 7;
  public static final int RCODE_DISPLAY_PROPERTIES = 8;
  public static final int FIRST_FREE_RCODE = 9;

  private final DataUtil du;
  private final Activity activity;
  private final DisplayActivity da;

  private final String appName;
  private final String tableId;
  private String sqlWhereClause;
  private String[] sqlSelectionArgs;
  private String[] sqlGroupBy;
  private String sqlHaving;
  private String sqlOrderByElementKey;
  private String sqlOrderByDirection;

  private TableProperties tp;
  private DbTable dbt;
  private final RelativeLayout container;
  private LinearLayout controlWrap;
  private ClearableEditText searchField;
  private TextView infoBar;
  private final ViewGroup displayWrap;
  private View overlay;
  private RelativeLayout.LayoutParams overlayLp;
  private TableViewType mCurrentViewType;


  public Controller(Activity activity, final DisplayActivity da, Bundle intentBundle, Bundle savedInstanceState) {
    du = new DataUtil(Locale.ENGLISH, TimeZone.getDefault());
    this.activity = activity;
    String tmpAppName = intentBundle.getString(INTENT_KEY_APP_NAME);
    if (tmpAppName == null) {
      this.appName = TableFileUtils.getDefaultAppName();
    } else {
      this.appName = tmpAppName;
    }
    this.da = da;
    // getting intent information
    tableId = intentBundle.getString(INTENT_KEY_TABLE_ID);
    if (tableId == null) {
      throw new RuntimeException();
    }

    sqlWhereClause = intentBundle.getString(INTENT_KEY_SQL_WHERE);
    sqlSelectionArgs = intentBundle.getStringArray(INTENT_KEY_SQL_SELECTION_ARGS);
    sqlGroupBy = intentBundle.getStringArray(INTENT_KEY_SQL_GROUP_BY_ARGS);
    sqlHaving = intentBundle.getString(INTENT_KEY_SQL_HAVING);
    sqlOrderByElementKey = intentBundle.getString(INTENT_KEY_SQL_ORDER_BY_ELEMENT_KEY);
    sqlOrderByDirection = intentBundle.getString(INTENT_KEY_SQL_ORDER_BY_DIRECTION);

    if ( savedInstanceState != null && savedInstanceState.containsKey(INTENT_KEY_CURRENT_VIEW_TYPE) ) {
      mCurrentViewType = TableViewType.valueOf(savedInstanceState.getString(INTENT_KEY_CURRENT_VIEW_TYPE));
    } else if (intentBundle.containsKey(INTENT_KEY_CURRENT_VIEW_TYPE)) {
      mCurrentViewType = TableViewType.valueOf(intentBundle.getString(INTENT_KEY_CURRENT_VIEW_TYPE));
    } else {
      mCurrentViewType = null;
    }

    // initializing data objects
    refreshDbTable(tableId);

    // INITIALIZING VIEW OBJECTS
    // controlWrap will hold the search bar and search button
    controlWrap = new LinearLayout(activity);
    // searchField is the search bar
    searchField = new ClearableEditText(activity);
    // displayWrap holds the spreadsheet/listView/etc
    displayWrap = new LinearLayout(activity);
    // container holds the entire view of the activity
    container = new RelativeLayout(activity);

    // BUILD VIEW OBJECTS
    // controlWrap is initialized to be hidden. clicking Action Item, search,
    // will show/hide it
    searchField.setId(VIEW_ID_SEARCH_FIELD);
    // TODO: use where clause to reconstruct this...
    searchField.getEditText().setText("");
    ImageButton searchButton = new ImageButton(activity);
    searchButton.setId(VIEW_ID_SEARCH_BUTTON);
    searchButton.setImageResource(R.drawable.ic_action_search);
    searchButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        // when you click the search button, save that query.
        Toast.makeText(Controller.this.activity, "Unimplemented", Toast.LENGTH_LONG).show();
      }
    });
    LinearLayout.LayoutParams searchFieldParams = new LinearLayout.LayoutParams(
                                                                                LinearLayout.LayoutParams.MATCH_PARENT,
                                                                                LinearLayout.LayoutParams.WRAP_CONTENT);
    searchFieldParams.weight = 1;
    controlWrap.addView(searchField, searchFieldParams);
    LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                                                                           LinearLayout.LayoutParams.WRAP_CONTENT,
                                                                           LinearLayout.LayoutParams.WRAP_CONTENT);
    buttonParams.weight = 0;
    controlWrap.addView(searchButton, buttonParams);
    controlWrap.setVisibility(View.GONE);

    // info bar currently displays just the name of the table
    infoBar = new TextView(activity);
    setSimpleInfoBarText();
    infoBar.setBackgroundColor(Color.parseColor("#B0B0B0"));
    infoBar.setTextColor(Color.BLACK);

    LinearLayout wrapper = new LinearLayout(activity);
    wrapper.setOrientation(LinearLayout.VERTICAL);
    LinearLayout.LayoutParams controlParams = new LinearLayout.LayoutParams(
                                                                            LinearLayout.LayoutParams.MATCH_PARENT,
                                                                            LinearLayout.LayoutParams.WRAP_CONTENT);
    wrapper.addView(controlWrap, controlParams);
    wrapper.addView(infoBar, controlParams);

    LinearLayout.LayoutParams displayParams = new LinearLayout.LayoutParams(
                                                                            LinearLayout.LayoutParams.MATCH_PARENT,
                                                                            LinearLayout.LayoutParams.MATCH_PARENT);
    wrapper.addView(displayWrap, displayParams);
    container.addView(wrapper, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                          ViewGroup.LayoutParams.MATCH_PARENT));
  }

  public void onSaveInstanceState(Bundle outState) {
    if ( mCurrentViewType != null ) {
      outState.putString(INTENT_KEY_CURRENT_VIEW_TYPE, mCurrentViewType.name());
    }
  }

  public TableViewType getCurrentViewType() {
    if ( mCurrentViewType == null ) {
      mCurrentViewType = tp.getDefaultViewType();
    }
    return mCurrentViewType;
  }

  public void setCurrentViewType(TableViewType viewType) {
    mCurrentViewType = viewType;
  }

  public void setSimpleInfoBarText() {
    infoBar.setText(activity.getString(R.string.info_bar_plain_title, tp.getLocalizedDisplayName()));
  }

  public void setListViewInfoBarText() {
    infoBar.setText(activity.getString(R.string.info_bar_list_title, tp.getLocalizedDisplayName()));
  }

  public void setDetailViewInfoBarText() {
    infoBar.setText(activity.getString(R.string.info_bar_detail_title, tp.getLocalizedDisplayName()));
  }

  public void setGraphViewInfoBarText(String graphName) {
    infoBar.setText(activity.getString(R.string.info_bar_graph_title, tp.getLocalizedDisplayName(),
        graphName));
  }

  /**
   * @return the current text in info bar
   */
  public String getInfoBarText() {
    return infoBar.getText().toString();
  }

  /**
   * @return TableProperties properties of this table
   */
  public TableProperties getTableProperties() {
    return tp;
  }

  /**
   * Update the dbTable that Controller is monitoring. This should be called
   * only if there is no way to update the dbTable held by the
   * Controller if a change happens outside of the Controller's realm of
   * control. For instance, changing a column display name in PropertyManager
   * does not get updated to the dbTable without calling this method. This is
   * a messy way of doing things, and a refactor should probably end up fixing
   * this.
   */
  public void refreshDbTable(String tableId) {
    tp = TableProperties.getTablePropertiesForTable(activity, appName, tableId);
    dbt = DbTable.getDbTable(tp);
  }

  /**
   * @return DbTable this data table
   */
  public DbTable getDbTable() {
    return dbt;
  }

  public String getSqlWhereClause() {
    return this.sqlWhereClause;
  }

  public String[] getSqlSelectionArgs() {
    return this.sqlSelectionArgs;
  }

  public boolean hasSqlGroupByClause() {
    return this.sqlGroupBy != null && this.sqlGroupBy.length != 0;
  }

  public String[] getSqlGroupBy() {
    return this.sqlGroupBy;
  }

  public String getSqlHaving() {
    return this.sqlHaving;
  }

  public String getSqlOrderByElementKey() {
    return this.sqlOrderByElementKey;
  }

  public String getSqlOrderByDirection() {
    return this.sqlOrderByDirection;
  }

  /**
   * @return the view generated for this
   */
  public View getContainerView() {
    return container;
  }

  public void setDisplayView(View dv) {
    displayWrap.removeAllViews();
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                                                     LinearLayout.LayoutParams.MATCH_PARENT,
                                                                     LinearLayout.LayoutParams.MATCH_PARENT);
    displayWrap.addView(dv, params);
  }

  void addOverlay(View overlay, int width, int height, int x, int y) {
    removeOverlay();
    this.overlay = overlay;
    overlayLp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                                ViewGroup.LayoutParams.WRAP_CONTENT);
    overlayLp.leftMargin = x;
    overlayLp.topMargin = y - activity.getActionBar().getHeight() - infoBar.getHeight();
    overlayLp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
    container.addView(overlay, overlayLp);
  }

  void removeOverlay() {
    if (overlay != null) {
      container.removeView(overlay);
      overlay = null;
      overlayLp = null;
    }
  }

  void setOverlayLocation(int x, int y) {
    overlayLp.leftMargin = x;
    overlayLp.topMargin = y - activity.getActionBar().getHeight() - infoBar.getHeight();
    container.requestLayout();
  }

  void releaseView(View v) {
    displayWrap.removeView(v);
  }

  boolean isInSearchBox(int x, int y) {
    Log.d("CNTRLR", "isInSearchBox(" + x + "," + y + ")");
    y -= controlWrap.getHeight();
    Rect bounds = new Rect();
    searchField.getHitRect(bounds);
    Log.d("CNTRLR", bounds.toString());
    return ((bounds.left <= x) && (bounds.right >= x) && (bounds.top <= y) && (bounds.bottom >= y));
  }

  /**
   * This is used to invert the color of the search box. The boolean parameter
   * specifies whether or not the color should be inverted or returned to
   * normal.
   * <p>
   * The inversion values are not tied to any particular theme, but are set
   * using the ActionBarSherlock themes. These need to change if the app themes
   * are changed.
   *
   * @param invert
   */
  void invertSearchBoxColor(boolean invert) {
    if (invert) {
      searchField.setBackgroundResource(android.R.color.background_light);
      searchField.getEditText().setTextColor(
          searchField.getContext().getResources().getColor(android.R.color.background_dark));
      searchField.getClearButton().setBackgroundResource(R.drawable.content_remove_dark);
    } else {
      searchField.setBackgroundResource(android.R.color.background_dark);
      searchField.getEditText().setTextColor(
          searchField.getContext().getResources().getColor(android.R.color.background_light));
      searchField.getClearButton().setBackgroundResource(R.drawable.content_remove_light);
    }
  }

  void appendToSearchBoxText(String text) {
    searchField.getEditText().setText((searchField.getEditText().getText() + text).trim());
  }

  /**
   * This should launch Collect to edit the data for the row. If there is a
   * custom form defined for the table, its info should be loaded in params.
   * If the formId in params is null, then the default form is generated, which
   * is just every column with its own entry field on a single screen.
   *
   * @param table
   * @param rowNum
   * @param params
   */
  /*
   * Examples for how this is done elsewhere can be found in:
   * Examples for how this is done in Collect can be found in the Collect code
   * in org.odk.collect.android.tasks.SaveToDiskTask.java, in the
   * updateInstanceDatabase() method.
   */
  void editRow(UserTable table, int rowNum) {
    FormType formType = FormType.constructFormType(getTableProperties());
    if ( formType.isCollectForm() ) {
      Map<String, String> elementKeyToValue = new HashMap<String, String>();
      for (ColumnProperties cp : tp.getDatabaseColumns().values()) {
        String value = table.getData(rowNum, tp.getColumnIndex(cp.getElementKey()));
        elementKeyToValue.put(cp.getElementKey(), value);
      }

      Intent intent = CollectUtil.getIntentForOdkCollectEditRow(activity, tp, elementKeyToValue,
          null, null, null, table.getRowAtIndex(rowNum).getRowId());

      if (intent != null) {
        CollectUtil.launchCollectToEditRow(activity, intent,
            table.getRowAtIndex(rowNum).getRowId());
      } else {
        Log.e(TAG, "intent null when trying to create for edit row.");
      }
    } else {
      SurveyFormParameters params = formType.getSurveyFormParameters();

      Intent intent = SurveyUtil.getIntentForOdkSurveyEditRow(activity, tp,
          appName, params, table.getRowAtIndex(rowNum).getRowId());
      if ( intent != null ) {
        SurveyUtil.launchSurveyToEditRow(activity, intent, tp,
            table.getRowAtIndex(rowNum).getRowId());
      }
    }
  }

  public boolean handleActivityReturn(int requestCode, int returnCode, Intent data) {
    switch (requestCode) {
    case RCODE_TABLE_PROPERTIES_MANAGER:
      handleTablePropertiesManagerReturn();
      return true;
    case RCODE_DISPLAY_PROPERTIES:
      handleDisplayPropertiesReturn();
      return true;
    case RCODE_COLUMN_MANAGER:
      handleColumnManagerReturn();
      return true;
    case RCODE_ODK_COLLECT_ADD_ROW:
      handleOdkCollectAddReturn(returnCode, data);
      return true;
    case RCODE_ODK_COLLECT_EDIT_ROW:
      handleOdkCollectEditReturn(returnCode, data);
      return true;
    case RCODE_ODK_SURVEY_ADD_ROW:
      handleOdkSurveyAddReturn(returnCode, data);
      return true;
    case RCODE_ODK_SURVEY_EDIT_ROW:
      handleOdkSurveyEditReturn(returnCode, data);
      return true;
    case RCODE_LIST_VIEW_MANAGER:
      handleListViewManagerReturn();
      return true;
    case RCODE_ODK_COLLECT_ADD_ROW_SPECIFIED_TABLE:
      handleOdkCollectAddReturnForSpecificTable(returnCode, data);
      return true;
    default:
      return false;
    }
  }

  private void handleListViewManagerReturn() {
    refreshDbTable(tp.getTableId());
    da.init();
  }

  private void handleTablePropertiesManagerReturn() {
    // so for now I think that the boolean of whether or not the current view
    // is an overview of a collection view is stored here in Controller.
    // This should eventually move, if we decide to keep this architecture. but
    // for now I'm going to just hardcode in a solution.
    TableViewType oldViewType = getCurrentViewType();
    refreshDbTable(tp.getTableId());
    if (oldViewType == tp.getDefaultViewType()) {
      da.init();
    } else {
      launchTableActivity(activity, tp, tp.getDefaultViewType(),
          sqlWhereClause, sqlSelectionArgs, sqlGroupBy, sqlHaving, sqlOrderByElementKey, sqlOrderByDirection);
    }
  }

  private void handleDisplayPropertiesReturn() {
    refreshDbTable(tp.getTableId());
    da.init();
  }

  private void handleColumnManagerReturn() {
    refreshDbTable(tp.getTableId());
    da.init();
  }

  void deleteRow(String rowId) {
    dbt.markDeleted(rowId);
  }

  /**
   * Builds the option menu (menus in the action bar and overflow)
   * with menu items enabled
   *
   * @param menu
   *          Menu
   */
  public void buildOptionsMenu(Menu menu) {
    this.buildOptionsMenu(menu, true);
  }

  /**
   * Builds the option menu (menus in the action bar and overflow)
   * Menu items can be enabled (true) or disabled (false)
   *
   * @param menu
   *          Menu
   * @param enabled
   *          boolean
   */
  void buildOptionsMenu(Menu menu, boolean enabled) {
    // set the app icon as an action to go home
    ActionBar actionBar = activity.getActionBar();
    actionBar.setDisplayHomeAsUpEnabled(true);

    // search
    MenuItem searchItem = menu.add(Menu.NONE, MENU_ITEM_ID_SEARCH_BUTTON, Menu.NONE,
        activity.getString(R.string.search));
    searchItem.setIcon(R.drawable.ic_action_search);
    searchItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    searchItem.setEnabled(enabled);

    // view type submenu
    // -determine the possible view types
    final TableViewType[] viewTypes = tp.getPossibleViewTypes();
    // -build a checkable submenu to select the view type
    SubMenu viewTypeSubMenu = menu.addSubMenu(Menu.NONE, MENU_ITEM_ID_VIEW_TYPE_SUBMENU, Menu.NONE,
        activity.getString(R.string.view_type));
    MenuItem viewType = viewTypeSubMenu.getItem();
    viewType.setIcon(R.drawable.view);
    viewType.setEnabled(enabled);
    viewType.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    MenuItem item;
    // This will be the name of the default list view, which if exists
    // means we should display the list view as an option.
    KeyValueStoreHelper kvsh = tp.getKeyValueStoreHelper(ListDisplayActivity.KVS_PARTITION);
    String nameOfView = kvsh.getString(ListDisplayActivity.KEY_LIST_VIEW_NAME);
    for (int i = 0; i < viewTypes.length; i++) {
      item = viewTypeSubMenu.add(MENU_ITEM_ID_VIEW_TYPE_SUBMENU, viewTypes[i].getId(), i,
          viewTypes[i].name());
      // mark the current viewType as selected
      if (getCurrentViewType() == viewTypes[i]) {
        item.setChecked(true);
      }
      // disable list view if no file is specified
      if (viewTypes[i] == TableViewType.List && nameOfView == null) {
        item.setEnabled(false);
      }
    }

    viewTypeSubMenu.setGroupCheckable(MENU_ITEM_ID_VIEW_TYPE_SUBMENU, true, true);

    // Add Row
    MenuItem addItem = menu.add(Menu.NONE, MENU_ITEM_ID_ADD_ROW_BUTTON, Menu.NONE,
        activity.getString(R.string.add_row)).setEnabled(enabled);
    addItem.setIcon(R.drawable.content_new);
    addItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

    // Settings submenu
    SubMenu settings = menu.addSubMenu(Menu.NONE, MENU_ITEM_ID_SETTINGS_SUBMENU, Menu.NONE,
        activity.getString(R.string.settings));
    MenuItem settingsItem = settings.getItem();
    settingsItem.setIcon(R.drawable.settings_icon2);
    settingsItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

    MenuItem display = settings.add(Menu.NONE, MENU_ITEM_ID_DISPLAY_PREFERENCES, Menu.NONE,
        activity.getString(R.string.display_prefs)).setEnabled(enabled);
    // always disable DisplayPreferences if it is currently in list view
    if (getCurrentViewType() == TableViewType.List)
      display.setEnabled(false);
    settings.add(Menu.NONE, MENU_ITEM_ID_OPEN_TABLE_PROPERTIES, Menu.NONE,
        activity.getString(R.string.table_props)).setEnabled(enabled);
    settings.add(Menu.NONE, MENU_ITEM_ID_OPEN_COLUMN_MANAGER, Menu.NONE,
        activity.getString(R.string.column_manager)).setEnabled(enabled);
    // Now an option for editing list views.
    settings.add(Menu.NONE, MENU_ITEM_ID_OPEN_LIST_VIEW_MANAGER,
        Menu.NONE, activity.getString(R.string.list_view_manager)).setEnabled(true);
  }

  /**
   * Handle menu item that was selected by user
   *
   * @param selectedItem
   *          MenuItem
   * @return true if selectedItem was handled
   */
  public boolean handleMenuItemSelection(MenuItem selectedItem) {
    int itemId = selectedItem.getItemId();
    // if the item is part of the sub-menu for view type, set the view type with
    // its itemId
    // else, handle accordingly
    if (selectedItem.getGroupId() == MENU_ITEM_ID_VIEW_TYPE_SUBMENU) {
      setCurrentViewType(TableViewType.getViewTypeFromId(selectedItem.getItemId()));
      Controller.launchTableActivity(activity, tp, null, getCurrentViewType(),
          sqlWhereClause, sqlSelectionArgs, sqlGroupBy, sqlHaving, sqlOrderByElementKey, sqlOrderByDirection);
      return true;
    } else {
      switch (itemId) {
      case MENU_ITEM_ID_SEARCH_BUTTON:
        int visible = controlWrap.getVisibility();
        if (visible == View.GONE)
          controlWrap.setVisibility(View.VISIBLE);
        else
          controlWrap.setVisibility(View.GONE);
        return true;
      case MENU_ITEM_ID_VIEW_TYPE_SUBMENU:
        return true;
      case MENU_ITEM_ID_ADD_ROW_BUTTON: {
        FormType formType = FormType.constructFormType(tp);
        if (formType.isCollectForm()) {
          CollectFormParameters params = formType.getCollectFormParameters();
          // Try to construct the values currently in the search bar to
          // prepopulate with the form. We're going to ignore joins. This
          // means that if there IS a join column, we'll throw an error!!!
          // So be careful.
          Intent intentAddRow =
              CollectUtil.getIntentForOdkCollectAddRow(
                  activity,
                  tp,
                  params, null);

          if (intentAddRow != null) {
            Controller.this.activity
                .startActivityForResult(intentAddRow, RCODE_ODK_COLLECT_ADD_ROW);
          }
        } else {
          SurveyFormParameters params = formType.getSurveyFormParameters();

          Intent intentAddRow = SurveyUtil.getIntentForOdkSurveyAddRow(activity, tp, appName,
              params, null);

          if (intentAddRow != null) {
            Controller.this.activity.startActivityForResult(intentAddRow, RCODE_ODK_SURVEY_ADD_ROW);
          }
        }
      }
        return true;
      case MENU_ITEM_ID_SETTINGS_SUBMENU:
        return true;
      case MENU_ITEM_ID_DISPLAY_PREFERENCES: {
        Intent intent = new Intent(activity, DisplayPrefsActivity.class);
        intent.putExtra(INTENT_KEY_APP_NAME, appName);
        intent.putExtra(DisplayPrefsActivity.INTENT_KEY_TABLE_ID, tp.getTableId());
        activity.startActivityForResult(intent, this.RCODE_DISPLAY_PROPERTIES);
      }
        return true;
      case MENU_ITEM_ID_OPEN_TABLE_PROPERTIES: {
        Intent intent = new Intent(activity, TablePropertiesManager.class);
        intent.putExtra(INTENT_KEY_APP_NAME, appName);
        intent.putExtra(INTENT_KEY_TABLE_ID, tp.getTableId());
        activity.startActivityForResult(intent, RCODE_TABLE_PROPERTIES_MANAGER);
      }
        return true;
      case MENU_ITEM_ID_OPEN_COLUMN_MANAGER: {
        Intent intent = new Intent(activity, ColumnManager.class);
        intent.putExtra(INTENT_KEY_APP_NAME, appName);
        intent.putExtra(INTENT_KEY_TABLE_ID, tp.getTableId());
        activity.startActivityForResult(intent, RCODE_COLUMN_MANAGER);
      }
        return true;
      case MENU_ITEM_ID_OPEN_LIST_VIEW_MANAGER: {
        Intent intent = new Intent(activity, ListViewManager.class);
        intent.putExtra(INTENT_KEY_APP_NAME, appName);
        intent.putExtra(INTENT_KEY_TABLE_ID, tp.getTableId());
        activity.startActivityForResult(intent, RCODE_LIST_VIEW_MANAGER);
      }
        return true;
      case android.R.id.home:
        Intent tableManagerIntent = new Intent(activity, TableManager.class);
        tableManagerIntent.putExtra(INTENT_KEY_APP_NAME, appName);
        // Add this flag so that you don't back from TableManager back
        // into the table.
        tableManagerIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        activity.startActivity(tableManagerIntent);
        activity.finish();
        return true;
      default:
        return false;
      }
    }
  }

  private void handleOdkCollectAddReturn(int returnCode, Intent data) {
    if (!CollectUtil.handleOdkCollectAddReturn(activity, appName, tp, returnCode, data)) {
      return;
    } else {
      // the add succeeded.
      da.init();
    }
  }

  private void handleOdkCollectEditReturn(int returnCode, Intent data) {
    if (!CollectUtil.handleOdkCollectEditReturn(activity, appName, tp, returnCode, data)) {
      return;
    } else {
      // The update succeeded.
      da.init();
    }
  }

  private void handleOdkSurveyAddReturn(int returnCode, Intent data) {
    if (returnCode != Activity.RESULT_OK) {
      Log.i(TAG, "return code wasn't OK, add was not finalized and will not appear.");
      return;
    }
    // the add succeeded.
    da.init();
  }

  private void handleOdkSurveyEditReturn(int returnCode, Intent data) {
    if (returnCode != Activity.RESULT_OK) {
      Log.i(TAG, "return code wasn't OK, edit not finalized and will not appear.");
      return;
    }
    // The update succeeded.
    da.init();
  }

  /**
   * Handle the add return from Collect if the user has specified a table
   * other than that which is currently held in the Controller. Note that the
   * Intent to launch Collect must have been launched using
   * {@link CollectUtil#launchCollectToAddRow(Activity, Intent, TableProperties)}
   * .
   *
   * @param returnCode
   * @param data
   */
  private void handleOdkCollectAddReturnForSpecificTable(int returnCode, Intent data) {
    String tableId = CollectUtil.retrieveAndRemoveTableIdForAddRow(activity);
    if (tableId == null) {
      Log.e(TAG, "return from ODK Collect expected to find a tableId "
          + "specifying the target of the add row, but was null.");
      return;
    }
    TableProperties tpToReceiveAdd = TableProperties.getTablePropertiesForTable(
        activity, appName, tableId);
    CollectUtil.handleOdkCollectAddReturn(activity, appName, tpToReceiveAdd, returnCode, data);
  }

  void openCellEditDialog(String rowId, String value, int colIndex) {
    (new CellEditDialog(rowId, value, colIndex)).show();
  }

  public static void launchTableActivity(Activity context, TableProperties tp, TableViewType viewType,
                                          String sqlWhereClause, String[] sqlSelectionArgs,
                                          String[] sqlGroupBy, String sqlHaving,
                                          String sqlOrderByElementKey, String sqlOrderByDirection) {
    Controller.launchTableActivity(context, tp, null, viewType,
        sqlWhereClause, sqlSelectionArgs, sqlGroupBy, sqlHaving, sqlOrderByElementKey, sqlOrderByDirection);
    context.finish();
  }

  /**
   * This is based on the other launch table activity methods. This one,
   * however, allows a filename to be passed to the launching activity. This is
   * intended to be used to launch things like list view activities with a file
   * other than the default.
   *
   * @param context
   * @param tp
   * @param searchStack
   * @param isOverview
   * @param filename
   */
  public static void launchTableActivityWithFilename(Activity context, TableProperties tp,
                                                      String filename, TableViewType viewType) {
    Controller.launchTableActivity(context, tp, filename, viewType,
        null, null, tp.getGroupByColumns().toArray(new String[0]), null, tp.getSortColumn(), tp.getSortOrder());
    context.finish();
  }

  public static void launchTableActivity(Context context, TableProperties tp, TableViewType viewType) {
    Controller.launchTableActivity(context, tp, null, viewType,
        null, null, tp.getGroupByColumns().toArray(new String[0]), null, tp.getSortColumn(), tp.getSortOrder());
  }

  /**
   * This method should launch the custom app view that is a generic user-
   * customizable home screen or html page for the app.
   */
  public static void launchAppViewActivity(Context context) {

  }

  /**
   * Launches the Table pointed to by tp as a list view with the specified
   * filename.
   *
   * @param context
   * @param tp
   * @param searchText
   * @param searchStack
   * @param isOverview
   * @param filename
   * @param sqlWhereClause
   * @param sqlSelectionArgs
   * @see DbTable#rawSqlQuery(String, String[])
   */
  public static void launchListView(Context context, TableProperties tp, String filename,
                                                String sqlWhereClause, String[] sqlSelectionArgs,
                                                String[] sqlGroupBy, String sqlHaving,
                                                String sqlOrderByElementKey, String sqlOrderByDirection) {
    Intent intent = new Intent(context, ListDisplayActivity.class);
    intent.putExtra(INTENT_KEY_APP_NAME, tp.getAppName());
    intent.putExtra(INTENT_KEY_TABLE_ID, tp.getTableId());
    if (filename != null) {
      intent.putExtra(ListDisplayActivity.INTENT_KEY_FILENAME, filename);
    }
    prepareIntentForLaunch(intent, tp, sqlWhereClause,
        sqlSelectionArgs, sqlGroupBy, sqlHaving, sqlOrderByElementKey, sqlOrderByDirection);
    context.startActivity(intent);
  }

  /**
   * Open the table to the map view.
   *
   * @param context
   * @param tp
   * @param searchText
   * @param searchStack
   * @param isOverview
   * @param sqlWhereClause
   * @param sqlSelectionArgs
   * @see DbTable#rawSqlQuery(String, String[])
   */
  public static void launchMapView(Context context, TableProperties tp, String filename,
                                    String sqlWhereClause, String[] sqlSelectionArgs,
                                    String[] sqlGroupBy, String sqlHaving,
                                    String sqlOrderByElementKey, String sqlOrderByDirection) {
    Intent intent = new Intent(context, TableActivity.class);
    intent.putExtra(INTENT_KEY_APP_NAME, tp.getAppName());
    intent.putExtra(INTENT_KEY_TABLE_ID, tp.getTableId());
    if (filename != null) {
      intent.putExtra(ListDisplayActivity.INTENT_KEY_FILENAME, filename);
    }
    prepareIntentForLaunch(intent, tp, sqlWhereClause,
        sqlSelectionArgs, sqlGroupBy, sqlHaving, sqlOrderByElementKey, sqlOrderByDirection);
    context.startActivity(intent);
  }

  public static void launchSpreadsheetView(Context context, TableProperties tp,
                                          String sqlWhereClause, String[] sqlSelectionArgs,
                                          String[] sqlGroupBy, String sqlHaving,
                                          String sqlOrderByElementKey, String sqlOrderByDirection) {
    Intent intent = new Intent(context, SpreadsheetDisplayActivity.class);
    intent.putExtra(INTENT_KEY_APP_NAME, tp.getAppName());
    intent.putExtra(INTENT_KEY_TABLE_ID, tp.getTableId());
    prepareIntentForLaunch(intent, tp, sqlWhereClause,
        sqlSelectionArgs, sqlGroupBy, sqlHaving, sqlOrderByElementKey, sqlOrderByDirection);
    context.startActivity(intent);
  }

  /**
   * Open the table to graph view.
   *
   * @param context
   * @param tp
   * @param searchText
   * @param searchStack
   * @param isOverview
   * @see DbTable#rawSqlQuery(String, String[])
   */
  public static void launchGraphView(Context context, TableProperties tp,
                                      String sqlWhereClause, String[] sqlSelectionArgs,
                                      String[] sqlGroupBy, String sqlHaving,
                                      String sqlOrderByElementKey, String sqlOrderByDirection) {
    Intent intent;
    String defaultGraph = GraphManagerActivity.getDefaultGraphName(tp);
    if ( defaultGraph != null ) {
      intent = new Intent(context, GraphDisplayActivity.class);
      intent.putExtra(GraphDisplayActivity.KEY_GRAPH_VIEW_NAME, defaultGraph);
    } else {
      intent = new Intent(context, GraphManagerActivity.class);
    }
    intent.putExtra(INTENT_KEY_APP_NAME, tp.getAppName());
    intent.putExtra(INTENT_KEY_TABLE_ID, tp.getTableId());
    prepareIntentForLaunch(intent, tp, sqlWhereClause,
        sqlSelectionArgs, sqlGroupBy, sqlHaving, sqlOrderByElementKey, sqlOrderByDirection);
    context.startActivity(intent);
  }

  /*
   * A helper method that was introduced just to eliminate redundant code.
   * Adds the appropriate extras to the intent.
   */
  private static void prepareIntentForLaunch(Intent intent, TableProperties tp,
                                             String sqlWhereClause, String[] sqlSelectionArgs,
                                             String[] sqlGroupBy, String sqlHaving,
                                             String sqlOrderByElementKey, String sqlOrderByDirection) {

    if (sqlWhereClause != null && sqlWhereClause.length() != 0) {
      intent.putExtra(INTENT_KEY_SQL_WHERE, sqlWhereClause);
      if (sqlSelectionArgs != null && sqlSelectionArgs.length != 0) {
        intent.putExtra(INTENT_KEY_SQL_SELECTION_ARGS, sqlSelectionArgs);
      }
    }
    if (sqlGroupBy != null && sqlGroupBy.length != 0) {
      intent.putExtra(INTENT_KEY_SQL_GROUP_BY_ARGS, sqlGroupBy);
      if (sqlHaving != null && sqlHaving.length() != 0) {
        intent.putExtra(INTENT_KEY_SQL_HAVING, sqlHaving);
      }
    }
    if (sqlOrderByElementKey != null && sqlOrderByElementKey.length() != 0) {
      intent.putExtra(INTENT_KEY_SQL_ORDER_BY_ELEMENT_KEY, sqlOrderByElementKey);
      if ( sqlOrderByDirection != null ) {
        intent.putExtra(INTENT_KEY_SQL_ORDER_BY_DIRECTION, sqlOrderByDirection);
      } else {
        intent.putExtra(INTENT_KEY_SQL_ORDER_BY_DIRECTION, "ASC");
      }
    }
  }

  private static void launchTableActivity(Context context, TableProperties tp,
                                          String filename, TableViewType viewType,
                                          String sqlWhereClause, String[] sqlSelectionArgs,
                                          String[] sqlGroupBy, String sqlHaving,
                                          String sqlOrderByElementKey, String sqlOrderByDirection) {
    if ( viewType == null ) {
      viewType = tp.getDefaultViewType();
    }
    Intent intent;
    switch (viewType) {
    case List: {
      if (filename != null) {
        intent = new Intent(context, ListDisplayActivity.class);
        intent.putExtra(ListDisplayActivity.INTENT_KEY_FILENAME, filename);
      } else {
        intent = new Intent(context, ListViewManager.class);
      }
    }
      break;
    case Graph: {
      String defaultGraph = GraphManagerActivity.getDefaultGraphName(tp);
      if ( defaultGraph != null ) {
        intent = new Intent(context, GraphDisplayActivity.class);
        intent.putExtra(GraphDisplayActivity.KEY_GRAPH_VIEW_NAME, defaultGraph);
      } else {
        intent = new Intent(context, GraphManagerActivity.class);
      }
    }
      break;
    case Map:
      intent = new Intent(context, TableActivity.class);
      break;
    case Spreadsheet:
      intent = new Intent(context, SpreadsheetDisplayActivity.class);
      break;
    default:
      intent = new Intent(context, SpreadsheetDisplayActivity.class);
    }
    intent.putExtra(INTENT_KEY_APP_NAME, tp.getAppName());
    intent.putExtra(INTENT_KEY_TABLE_ID, tp.getTableId());
    prepareIntentForLaunch(intent, tp, sqlWhereClause,
        sqlSelectionArgs, sqlGroupBy, sqlHaving, sqlOrderByElementKey, sqlOrderByDirection);
    context.startActivity(intent);
  }

  /**
   * Launch a detail view for the given table showing the given rowNum.
   *
   * @param activity
   * @param appName
   * @param tableId
   * @param rowId
   * @param filename
   *          the filename to be used if the filename differs than that
   *          set in the key value store.
   */
  public static void launchDetailActivity(Activity activity,
      String appName, String tableId, String rowId, String filename) {
    Intent intent = new Intent(activity, DetailDisplayActivity.class);
    intent.putExtra(INTENT_KEY_APP_NAME, appName);
    intent.putExtra(INTENT_KEY_TABLE_ID, tableId);
    intent.putExtra(DetailDisplayActivity.INTENT_KEY_ROW_ID, rowId);
    if (filename != null) {
      // a null value informs the DetailDisplayActivity that the filename in
      // the kvs should be used, so only add it if it has been set.
      intent.putExtra(DetailDisplayActivity.INTENT_KEY_FILENAME, filename);
    }
    activity.startActivity(intent);
  }

  private class CellEditDialog extends AlertDialog {

    private final String rowId;
    private final int colIndex;
    private final String elementKey;
    private final CellValueView.CellEditView cev;

    public CellEditDialog(String rowId, String value, int colIndex) {
      super(activity);
      this.rowId = rowId;
      this.colIndex = colIndex;
      ColumnProperties cp = tp.getColumnByIndex(colIndex);
      this.elementKey = cp.getElementKey();
      cev = CellValueView.getCellEditView(activity, cp, value);
      buildView(activity);
    }

    private void buildView(Context context) {
      Button setButton = new Button(context);
      setButton.setText(activity.getResources().getString(R.string.set));
      setButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          String value = du.validifyValue(tp.getColumnByElementKey(CellEditDialog.this.elementKey),
              cev.getValue());
          if (value == null) {
            // TODO: alert the user
            return;
          }
          Map<String, String> values = new HashMap<String, String>();
          values.put(CellEditDialog.this.elementKey, value);

          // TODO: supply reasonable values for these...
          String savepointCreator = null; // user on phone
          Long timestamp = null; // current time
          String formId = null; // formId used by ODK Collect
          String locale = null; // current locale

          dbt.updateRow(rowId, formId, locale, timestamp, savepointCreator, values);
          da.init();
          dismiss();
        }
      });
      Button cancelButton = new Button(context);
      cancelButton.setText(activity.getResources().getString(R.string.cancel));
      cancelButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          dismiss();
        }
      });
      LinearLayout buttonWrapper = new LinearLayout(context);
      buttonWrapper.addView(setButton);
      buttonWrapper.addView(cancelButton);
      LinearLayout wrapper = new LinearLayout(context);
      wrapper.setOrientation(LinearLayout.VERTICAL);
      wrapper.addView(cev);
      wrapper.addView(buttonWrapper);
      setView(wrapper);
    }
  }

}
