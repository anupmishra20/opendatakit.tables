package yoonsung.odk.spreadsheet.Activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import yoonsung.odk.spreadsheet.Activity.util.LanguageUtil;
import yoonsung.odk.spreadsheet.Activity.util.SecurityUtil;
import yoonsung.odk.spreadsheet.Activity.util.ShortcutUtil;
import yoonsung.odk.spreadsheet.data.ColumnProperties;
import yoonsung.odk.spreadsheet.data.DbHelper;
import yoonsung.odk.spreadsheet.data.TableProperties;
import yoonsung.odk.spreadsheet.data.TableViewSettings;
import yoonsung.odk.spreadsheet.data.TableViewSettings.ConditionalRuler;

/**
 * An activity for managing a table's properties.
 * 
 * @author hkworden@gmail.com
 */
public class TablePropertiesManager extends PreferenceActivity {
    
    public static final String INTENT_KEY_TABLE_ID = "tableId";
    
    private static final int RC_DETAIL_VIEW_FILE = 0;
    
    private enum ViewPreferenceType {
        OVERVIEW_VIEW,
        COLLECTION_VIEW
    }
    
    private DbHelper dbh;
    private TableProperties tp;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String tableId = getIntent().getStringExtra(INTENT_KEY_TABLE_ID);
        if (tableId == null) {
            throw new RuntimeException("Table ID (" + tableId +
                    ") is invalid.");
        }
        dbh = DbHelper.getDbHelper(this);
        tp = TableProperties.getTablePropertiesForTable(dbh, tableId);
        setTitle("ODK Tables > Table Manager > " + tp.getDisplayName());
        init();
    }
    
    private void init() {
        PreferenceScreen root =
            getPreferenceManager().createPreferenceScreen(this);
        
        // general category
        
        PreferenceCategory genCat = new PreferenceCategory(this);
        root.addPreference(genCat);
        genCat.setTitle("General");
        
        EditTextPreference dnPref = new EditTextPreference(this);
        dnPref.setTitle("Display Name");
        dnPref.setDialogTitle("Change Display Name");
        dnPref.setText(tp.getDisplayName());
        dnPref.setSummary(tp.getDisplayName());
        dnPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference,
                    Object newValue) {
                tp.setDisplayName((String) newValue);
                setTitle("ODK Tables > Table Manager > " +
                        tp.getDisplayName());
                init();
                return false;
            }
        });
        genCat.addPreference(dnPref);
        
        boolean canBeAccessTable = SecurityUtil.couldBeSecurityTable(
                tp.getColumnOrder());
        boolean canBeShortcutTable = ShortcutUtil.couldBeShortcutTable(
                tp.getColumnOrder());
        int tableTypeCount = 1 + (canBeAccessTable ? 1 : 0) +
                (canBeShortcutTable ? 1 : 0);
        String[] tableTypeIds = new String[tableTypeCount];
        String[] tableTypeNames = new String[tableTypeCount];
        tableTypeIds[0] = String.valueOf(TableProperties.TableType.DATA);
        tableTypeNames[0] = LanguageUtil.getTableTypeLabel(
                TableProperties.TableType.DATA);
        if (canBeAccessTable) {
            tableTypeIds[1] = String.valueOf(
                    TableProperties.TableType.SECURITY);
            tableTypeNames[1] = LanguageUtil.getTableTypeLabel(
                    TableProperties.TableType.SECURITY);
        }
        if (canBeShortcutTable) {
            int index = canBeAccessTable ? 2 : 1;
            tableTypeIds[index] = String.valueOf(
                    TableProperties.TableType.SHORTCUT);
            tableTypeNames[index] = LanguageUtil.getTableTypeLabel(
                    TableProperties.TableType.SHORTCUT);
        }
        ListPreference tableTypePref = new ListPreference(this);
        tableTypePref.setTitle("Table Type");
        tableTypePref.setDialogTitle("Change Table Type");
        tableTypePref.setEntryValues(tableTypeIds);
        tableTypePref.setEntries(tableTypeNames);
        tableTypePref.setValue(String.valueOf(tp.getTableType()));
        tableTypePref.setSummary(LanguageUtil.getTableTypeLabel(
                tp.getTableType()));
        tableTypePref.setOnPreferenceChangeListener(
                new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference,
                    Object newValue) {
                tp.setTableType(Integer.parseInt((String) newValue));
                init();
                return false;
            }
        });
        genCat.addPreference(tableTypePref);
        
        // display category
        
        PreferenceCategory displayCat = new PreferenceCategory(this);
        root.addPreference(displayCat);
        displayCat.setTitle("Display");
        
        addViewPreferences(ViewPreferenceType.OVERVIEW_VIEW, displayCat);
        addViewPreferences(ViewPreferenceType.COLLECTION_VIEW, displayCat);
        
        FileSelectorPreference detailViewPref =
                new FileSelectorPreference(this);
        detailViewPref.setTitle("Detail View File");
        detailViewPref.setDialogTitle("Change Detail View File");
        detailViewPref.setText(tp.getDetailViewFilename());
        detailViewPref.setOnPreferenceChangeListener(
                new OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference,
                            Object newValue) {
                        tp.setDetailViewFilename((String) newValue);
                        init();
                        return false;
                    }
        });
        displayCat.addPreference(detailViewPref);
        
        // security category
        
        PreferenceCategory securityCat = new PreferenceCategory(this);
        root.addPreference(securityCat);
        securityCat.setTitle("Access Control");
        
        TableProperties[] accessTps =
            TableProperties.getTablePropertiesForSecurityTables(dbh);
        int accessTableCount =
                (tp.getTableType() == TableProperties.TableType.SECURITY) ?
                (accessTps.length) : accessTps.length + 1;
        TableProperties readTp = null;
        TableProperties writeTp = null;
        String[] accessTableIds = new String[accessTableCount];
        accessTableIds[0] = "-1";
        String[] accessTableNames = new String[accessTableCount];
        accessTableNames[0] = "None";
        int index = 1;
        for (TableProperties accessTp : accessTps) {
            if (accessTp.getTableId().equals(tp.getTableId())) {
                continue;
            }
            if ((tp.getReadSecurityTableId() != null) &&
                    accessTp.getTableId().equals(
                    tp.getReadSecurityTableId())) {
                readTp = accessTp;
            }
            if ((tp.getWriteSecurityTableId() != null) &&
                    accessTp.getTableId().equals(
                    tp.getWriteSecurityTableId())) {
                writeTp = accessTp;
            }
            accessTableIds[index] = String.valueOf(accessTp.getTableId());
            accessTableNames[index] = accessTp.getDisplayName();
            index++;
        }
        
        ListPreference readTablePref = new ListPreference(this);
        readTablePref.setTitle("Read Access Table");
        readTablePref.setDialogTitle("Change Read Access Table");
        readTablePref.setEntryValues(accessTableIds);
        readTablePref.setEntries(accessTableNames);
        if (readTp == null) {
            readTablePref.setValue("-1");
            readTablePref.setSummary("None");
        } else {
            readTablePref.setValue(String.valueOf(readTp.getTableId()));
            readTablePref.setSummary(readTp.getDisplayName());
        }
        readTablePref.setOnPreferenceChangeListener(
                new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference,
                    Object newValue) {
                tp.setReadSecurityTableId((String) newValue);
                init();
                return false;
            }
        });
        securityCat.addPreference(readTablePref);
        
        ListPreference writeTablePref = new ListPreference(this);
        writeTablePref.setTitle("Write Access Table");
        writeTablePref.setDialogTitle("Change Write Access Table");
        writeTablePref.setEntryValues(accessTableIds);
        writeTablePref.setEntries(accessTableNames);
        if (writeTp == null) {
            writeTablePref.setValue("-1");
            writeTablePref.setSummary("None");
        } else {
            writeTablePref.setValue(String.valueOf(writeTp.getTableId()));
            writeTablePref.setSummary(writeTp.getDisplayName());
        }
        writeTablePref.setOnPreferenceChangeListener(
                new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference,
                    Object newValue) {
                tp.setWriteSecurityTableId((String) newValue);
                init();
                return false;
            }
        });
        securityCat.addPreference(writeTablePref);
        
        setPreferenceScreen(root);
    }
    
    private void addViewPreferences(final ViewPreferenceType type,
            PreferenceCategory prefCat) {
        final TableViewSettings settings;
        String label;
        switch (type) {
        case OVERVIEW_VIEW:
            settings = tp.getOverviewViewSettings();
            label = "Overview View";
            break;
        case COLLECTION_VIEW:
            settings = tp.getCollectionViewSettings();
            label = "Collection View";
            break;
        default:
            throw new RuntimeException();
        }
        
        final List<ColumnProperties> numberCols =
            new ArrayList<ColumnProperties>();
        final List<ColumnProperties> locationCols =
            new ArrayList<ColumnProperties>();
        for (ColumnProperties cp : tp.getColumns()) {
            if (cp.getColumnType() == ColumnProperties.ColumnType.NUMBER) {
                numberCols.add(cp);
            } else if (cp.getColumnType() ==
                    ColumnProperties.ColumnType.LOCATION) {
                locationCols.add(cp);
            }
        }
        
        int[] viewTypes = settings.getPossibleViewTypes();
        String[] viewTypeIds = new String[viewTypes.length];
        String[] viewTypeNames = new String[viewTypes.length];
        for (int i = 0; i < viewTypes.length; i++) {
            int viewType = viewTypes[i];
            viewTypeIds[i] = String.valueOf(viewType);
            viewTypeNames[i] = LanguageUtil.getViewTypeLabel(viewType);
        }
        ListPreference viewTypePref = new ListPreference(this);
        viewTypePref.setTitle(label + " Type");
        viewTypePref.setDialogTitle("Change " + label + " Type");
        viewTypePref.setEntryValues(viewTypeIds);
        viewTypePref.setEntries(viewTypeNames);
        viewTypePref.setValue(String.valueOf(settings.getViewType()));
        viewTypePref.setSummary(LanguageUtil.getViewTypeLabel(
                settings.getViewType()));
        viewTypePref.setOnPreferenceChangeListener(
                new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference,
                    Object newValue) {
                int viewType = Integer.valueOf((String) newValue);
                settings.setViewType(viewType);
                init();
                return false;
            }
        });
        prefCat.addPreference(viewTypePref);
        
        switch (settings.getViewType()) {
        
        case TableViewSettings.Type.LIST:
            {
            EditTextPreference formatPref = new EditTextPreference(this);
            formatPref.setTitle(label + " List Format");
            formatPref.setDialogTitle("Change " + label + " List Format");
            if (settings.getListFormat() != null) {
                formatPref.setDefaultValue(settings.getListFormat());
            }
            formatPref.setOnPreferenceChangeListener(
                    new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference,
                        Object newValue) {
                    settings.setListFormat((String) newValue);
                    init();
                    return false;
                }
            });
            prefCat.addPreference(formatPref);
            }
            break;
        
        case TableViewSettings.Type.LINE_GRAPH:
            {
            ColumnProperties xCol = settings.getLineXCol();
            if (xCol == null) {
                xCol = numberCols.get(0);
            }
            ColumnProperties yCol = settings.getLineYCol();
            if (yCol == null) {
                yCol = numberCols.get(0);
            }
            String[] numberColDbNames = new String[numberCols.size()];
            String[] numberColDisplayNames = new String[numberCols.size()];
            for (int i = 0; i < numberCols.size(); i++) {
                numberColDbNames[i] = numberCols.get(i).getColumnDbName();
                numberColDisplayNames[i] = numberCols.get(i).getDisplayName();
            }
            
            ListPreference lineXColPref = new ListPreference(this);
            lineXColPref.setTitle(label + " X Column");
            lineXColPref.setDialogTitle("Change " + label + " X Column");
            lineXColPref.setEntryValues(numberColDbNames);
            lineXColPref.setEntries(numberColDisplayNames);
            lineXColPref.setValue(xCol.getColumnDbName());
            lineXColPref.setSummary(xCol.getDisplayName());
            lineXColPref.setOnPreferenceChangeListener(
                    new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference,
                        Object newValue) {
                    settings.setLineXCol(tp.getColumnByDbName(
                            (String) newValue));
                    init();
                    return false;
                }
            });
            prefCat.addPreference(lineXColPref);
            
            ListPreference lineYColPref = new ListPreference(this);
            lineYColPref.setTitle(label + " Y Column");
            lineYColPref.setDialogTitle("Change " + label + " Y Column");
            lineYColPref.setEntryValues(numberColDbNames);
            lineYColPref.setEntries(numberColDisplayNames);
            lineYColPref.setValue(yCol.getColumnDbName());
            lineYColPref.setSummary(yCol.getDisplayName());
            lineYColPref.setOnPreferenceChangeListener(
                    new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference,
                        Object newValue) {
                    settings.setLineYCol(tp.getColumnByDbName(
                            (String) newValue));
                    init();
                    return false;
                }
            });
            prefCat.addPreference(lineYColPref);
            }
            break;
        
        case TableViewSettings.Type.BAR_GRAPH:
            {
            ColumnProperties xCol = settings.getBarXCol();
            if (xCol == null) {
                xCol = tp.getColumns()[0];
            }
            String yCol = settings.getBarYCol();
            String yColSummary;
            if (yCol == null || yCol.startsWith("*")) {
                yCol = "*count";
                yColSummary = "Count";
            } else {
                yColSummary = tp.getColumnByDbName(yCol).getDisplayName();
            }
            ColumnProperties[] cps = tp.getColumns();
            String[] xColDbNames = new String[cps.length];
            String[] xColDisplayNames = new String[cps.length];
            for (int i = 0; i < cps.length; i++) {
                xColDbNames[i] = cps[i].getColumnDbName();
                xColDisplayNames[i] = cps[i].getDisplayName();
            }
            String[] yColDbNames = new String[numberCols.size() + 1];
            String[] yColDisplayNames = new String[numberCols.size() + 1];
            yColDbNames[0] = "*count";
            yColDisplayNames[0] = "Count";
            for (int i = 0; i < numberCols.size(); i++) {
                yColDbNames[i] = numberCols.get(i).getColumnDbName();
                yColDisplayNames[i] = numberCols.get(i).getDisplayName();
            }
            
            ListPreference barXColPref = new ListPreference(this);
            barXColPref.setTitle(label + " X Column");
            barXColPref.setDialogTitle("Change " + label + " X Column");
            barXColPref.setEntryValues(xColDbNames);
            barXColPref.setEntries(xColDisplayNames);
            barXColPref.setValue(xCol.getColumnDbName());
            barXColPref.setSummary(xCol.getDisplayName());
            barXColPref.setOnPreferenceChangeListener(
                    new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference,
                        Object newValue) {
                    settings.setBarXCol(tp.getColumnByDbName(
                            (String) newValue));
                    init();
                    return false;
                }
            });
            prefCat.addPreference(barXColPref);
            
            ListPreference barYColPref = new ListPreference(this);
            barYColPref.setTitle(label + " Y Column");
            barYColPref.setDialogTitle("Change " + label + " Y Column");
            barYColPref.setEntryValues(yColDbNames);
            barYColPref.setEntries(yColDisplayNames);
            barYColPref.setValue(yCol);
            barYColPref.setSummary(yColSummary);
            barYColPref.setOnPreferenceChangeListener(
                    new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference,
                        Object newValue) {
                    settings.setBarYCol((String) newValue);
                    init();
                    return false;
                }
            });
            prefCat.addPreference(barYColPref);
            }
            break;
        
        case TableViewSettings.Type.BOX_STEM:
            {
            ColumnProperties xCol = settings.getBoxStemXCol();
            if (xCol == null) {
                xCol = tp.getColumns()[0];
            }
            ColumnProperties yCol = settings.getBoxStemYCol();
            if (yCol == null) {
                yCol = numberCols.get(0);
            }
            ColumnProperties[] cps = tp.getColumns();
            String[] colDbNames = new String[cps.length];
            String[] colDisplayNames = new String[cps.length];
            for (int i = 0; i < cps.length; i++) {
                colDbNames[i] = cps[i].getColumnDbName();
                colDisplayNames[i] = cps[i].getDisplayName();
            }
            String[] numberColDbNames = new String[numberCols.size()];
            String[] numberColDisplayNames = new String[numberCols.size()];
            for (int i = 0; i < numberCols.size(); i++) {
                numberColDbNames[i] = numberCols.get(i).getColumnDbName();
                numberColDisplayNames[i] = numberCols.get(i).getDisplayName();
            }
            
            ListPreference boxStemXColPref = new ListPreference(this);
            boxStemXColPref.setTitle(label + " X Column");
            boxStemXColPref.setDialogTitle("Change " + label + " X Column");
            boxStemXColPref.setEntryValues(colDbNames);
            boxStemXColPref.setEntries(colDisplayNames);
            boxStemXColPref.setValue(xCol.getColumnDbName());
            boxStemXColPref.setSummary(xCol.getDisplayName());
            boxStemXColPref.setOnPreferenceChangeListener(
                    new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference,
                        Object newValue) {
                    settings.setBoxStemXCol(tp.getColumnByDbName(
                            (String) newValue));
                    init();
                    return false;
                }
            });
            prefCat.addPreference(boxStemXColPref);
            
            ListPreference boxStemYColPref = new ListPreference(this);
            boxStemYColPref.setTitle(label + " Y Column");
            boxStemYColPref.setDialogTitle("Change " + label + " Y Column");
            boxStemYColPref.setEntryValues(numberColDbNames);
            boxStemYColPref.setEntries(numberColDisplayNames);
            boxStemYColPref.setValue(yCol.getColumnDbName());
            boxStemYColPref.setSummary(yCol.getDisplayName());
            boxStemYColPref.setOnPreferenceChangeListener(
                    new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference,
                        Object newValue) {
                    settings.setBoxStemYCol(tp.getColumnByDbName(
                            (String) newValue));
                    init();
                    return false;
                }
            });
            prefCat.addPreference(boxStemYColPref);
            }
            break;
        
        case TableViewSettings.Type.MAP:
            {
            ColumnProperties labelCol = settings.getMapLabelCol();
            if (labelCol == null) {
                settings.setMapLabelCol(tp.getColumns()[0]);
                labelCol = tp.getColumns()[0];
            }
            ColumnProperties locCol = settings.getMapLocationCol();
            if (locCol == null) {
                settings.setMapLocationCol(locationCols.get(0));
                locCol = locationCols.get(0);
            }
            ColumnProperties[] cps = tp.getColumns();
            String[] colDbNames = new String[cps.length];
            String[] colDisplayNames = new String[cps.length];
            for (int i = 0; i < cps.length; i++) {
                colDbNames[i] = cps[i].getColumnDbName();
                colDisplayNames[i] = cps[i].getDisplayName();
            }
            String[] locColDbNames = new String[locationCols.size()];
            String[] locColDisplayNames = new String[locationCols.size()];
            for (int i = 0; i < locationCols.size(); i++) {
                locColDbNames[i] = locationCols.get(i).getColumnDbName();
                locColDisplayNames[i] = locationCols.get(i).getDisplayName();
            }
            
            ListPreference mapLocPref = new ListPreference(this);
            mapLocPref.setTitle(label + " Location Column");
            mapLocPref.setDialogTitle("Change " + label + " Location Column");
            mapLocPref.setEntryValues(locColDbNames);
            mapLocPref.setEntries(locColDisplayNames);
            mapLocPref.setValue(locCol.getColumnDbName());
            mapLocPref.setSummary(locCol.getDisplayName());
            mapLocPref.setOnPreferenceChangeListener(
                    new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference,
                        Object newValue) {
                    settings.setMapLocationCol(tp.getColumnByDbName(
                            (String) newValue));
                    init();
                    return false;
                }
            });
            prefCat.addPreference(mapLocPref);
            
            ListPreference mapLabelPref = new ListPreference(this);
            mapLabelPref.setTitle(label + " Label Column");
            mapLabelPref.setDialogTitle("Change " + label + " Label Column");
            mapLabelPref.setEntryValues(colDbNames);
            mapLabelPref.setEntries(colDisplayNames);
            mapLabelPref.setValue(labelCol.getColumnDbName());
            mapLabelPref.setSummary(labelCol.getDisplayName());
            mapLabelPref.setOnPreferenceChangeListener(
                    new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference,
                        Object newValue) {
                    settings.setMapLabelCol(tp.getColumnByDbName(
                            (String) newValue));
                    init();
                    return false;
                }
            });
            prefCat.addPreference(mapLabelPref);
            
            String[] mapColorLabels =
                new String[TableViewSettings.MAP_COLOR_OPTIONS.length];
            for (int i = 0; i < TableViewSettings.MAP_COLOR_OPTIONS.length;
                    i++) {
                mapColorLabels[i] = LanguageUtil.getMapColorLabel(
                        TableViewSettings.MAP_COLOR_OPTIONS[i]);
            }
            Map<ColumnProperties, ConditionalRuler> colorRulers =
                new HashMap<ColumnProperties, ConditionalRuler>();
            for (ColumnProperties cp : tp.getColumns()) {
                colorRulers.put(cp, settings.getMapColorRuler(cp));
            }
            ConditionalRulerDialogPreference ccPref =
                new ConditionalRulerDialogPreference(
                        TableViewSettings.MAP_COLOR_OPTIONS, mapColorLabels,
                        colorRulers);
            ccPref.setTitle(label + " Map Color Options");
            prefCat.addPreference(ccPref);
            }
            break;
        
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
            Intent data) {
        if (resultCode == RESULT_CANCELED) {
            return;
        }
        switch (requestCode) {
        case RC_DETAIL_VIEW_FILE:
            Uri fileUri = data.getData();
            String filename = fileUri.getPath();
            tp.setDetailViewFilename(filename);
            init();
            break;
        default:
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
    
    @Override
    public void onBackPressed() {
        setResult(RESULT_OK);
        finish();
    }
    
    private class FileSelectorPreference extends EditTextPreference {
        
        FileSelectorPreference(Context context) {
            super(context);
        }
        
        @Override
        protected void onClick() {
            if (hasFilePicker()) {
                Intent intent = new Intent("org.openintents.action.PICK_FILE");
                if (getText() != null) {
                    intent.setData(Uri.parse("file:///" + getText()));
                }
                startActivityForResult(intent, RC_DETAIL_VIEW_FILE);
            } else {
                super.onClick();
            }
        }
        
        private boolean hasFilePicker() {
            PackageManager packageManager = getPackageManager();
            Intent intent = new Intent("org.openintents.action.PICK_FILE");
            List<ResolveInfo> list = packageManager.queryIntentActivities(
                    intent, PackageManager.MATCH_DEFAULT_ONLY);
            return (list.size() > 0);
        }
    }
    
    private class ConditionalRulerDialogPreference extends Preference {
        
        private final Dialog dialog;
        
        public ConditionalRulerDialogPreference(int[] values, String[] labels,
                Map<ColumnProperties, ConditionalRuler> rulerMap) {
            super(TablePropertiesManager.this);
            dialog = new ConditionalRulerDialog(values, labels, rulerMap);
        }
        
        @Override
        protected void onClick() {
            dialog.show();
        }
    }
    
    public class ConditionalRulerDialog extends Dialog {
        
        private final int[] values;
        private final String[] labels;
        private final Map<ColumnProperties, ConditionalRuler> rulerMap;
        private final ColumnProperties[] columns;
        private final String[] comparatorLabels;
        private final String[] columnDisplays;
        private LinearLayout ruleList;
        
        public ConditionalRulerDialog(int[] values, String[] labels,
                Map<ColumnProperties, ConditionalRuler> rulerMap) {
            super(TablePropertiesManager.this);
            this.values = values;
            this.labels = labels;
            this.rulerMap = rulerMap;
            columns = new ColumnProperties[tp.getColumns().length];
            comparatorLabels = new String[ConditionalRuler.Comparator.COUNT];
            for (int i = 0; i < comparatorLabels.length; i++) {
                comparatorLabels[i] =
                    LanguageUtil.getTvsConditionalComparator(i);
            }
            columnDisplays = new String[tp.getColumns().length];
            for (int i = 0; i < tp.getColumns().length; i++) {
                ColumnProperties cp = tp.getColumns()[i];
                columns[i] = cp;
                columnDisplays[i] = cp.getDisplayName();
            }
            setContentView(buildView());
        }
        
        private View buildView() {
            ruleList = new LinearLayout(TablePropertiesManager.this);
            ruleList.setOrientation(LinearLayout.VERTICAL);
            String[] comparatorValues =
                new String[ConditionalRuler.Comparator.COUNT];
            String[] comparatorDisplays =
                new String[ConditionalRuler.Comparator.COUNT];
            for (int i = 0; i < ConditionalRuler.Comparator.COUNT; i++) {
                comparatorValues[i] = String.valueOf(i);
                comparatorDisplays[i] =
                    LanguageUtil.getTvsConditionalComparator(i);
            }
            LinearLayout.LayoutParams rwLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.FILL_PARENT,
                    LinearLayout.LayoutParams.FILL_PARENT);
            Context context = TablePropertiesManager.this;
            for (ColumnProperties cp : tp.getColumns()) {
                ConditionalRuler cr = rulerMap.get(cp);
                for (int i = 0; i < cr.getRuleCount(); i++) {
                    ruleList.addView(buildRuleView(cp, cr, i), rwLp);
                }
            }
            LinearLayout controlWrapper = new LinearLayout(context);
            Button addButton = new Button(context);
            addButton.setText("Add");
            addButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ColumnProperties cp = columns[0];
                    ConditionalRuler cr = rulerMap.get(cp);
                    cr.addRule(ConditionalRuler.Comparator.EQUALS, "",
                            values[0]);
                    ruleList.addView(buildRuleView(cp, cr,
                            cr.getRuleCount() - 1));
                }
            });
            controlWrapper.addView(addButton);
            Button closeButton = new Button(context);
            closeButton.setText("Close");
            closeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();
                }
            });
            controlWrapper.addView(closeButton);
            LinearLayout wrapper = new LinearLayout(context);
            wrapper.setOrientation(LinearLayout.VERTICAL);
            wrapper.addView(ruleList);
            wrapper.addView(controlWrapper);
            return wrapper;
        }
        
        private View buildRuleView(final ColumnProperties cp,
                final ConditionalRuler cr, final int ruleIndex) {
            Context context = TablePropertiesManager.this;
            
            final Spinner colSpinner = getSpinner(context, columnDisplays);
            int columnIndex = -1;
            for (int i = 0; i < columns.length; i++) {
                if (cp == columns[i]) {
                    columnIndex = i;
                    break;
                }
            }
            if (columnIndex == -1) {
                throw new RuntimeException();
            }
            colSpinner.setSelection(columnIndex);
            
            final Spinner settingSpinner = getSpinner(context, labels);
            int setting = cr.getRuleSetting(ruleIndex);
            int settingIndex = -1;
            for (int i = 0; i < values.length; i++) {
                if (setting == values[i]) {
                    settingIndex = i;
                    break;
                }
            }
            if (settingIndex == -1) {
                throw new RuntimeException();
            }
            settingSpinner.setSelection(settingIndex);
            
            final Spinner compSpinner = getSpinner(context, comparatorLabels);
            compSpinner.setSelection(cr.getRuleComparator(ruleIndex));
            
            final EditText valueEt = new EditText(context);
            valueEt.setText(cr.getRuleValue(ruleIndex));
            
            Button deleteButton = new Button(context);
            deleteButton.setText("Delete");
            
            colSpinner.setOnItemSelectedListener(
                    new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view,
                        int position, long id) {
                    ColumnProperties nextCp = columns[position];
                    if (cp == nextCp) {
                        return;
                    }
                    cr.deleteRule(ruleIndex);
                    rulerMap.get(nextCp).addRule(
                            compSpinner.getSelectedItemPosition(),
                            valueEt.getText().toString(),
                            values[settingSpinner.getSelectedItemPosition()]);
                    setContentView(buildView());
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
            settingSpinner.setOnItemSelectedListener(
                    new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view,
                        int position, long id) {
                    if (cr.getRuleSetting(ruleIndex) == values[position]) {
                        return;
                    }
                    cr.setRuleSetting(ruleIndex, values[position]);
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
            compSpinner.setOnItemSelectedListener(
                    new AdapterView.OnItemSelectedListener() {
                        @Override
                public void onItemSelected(AdapterView<?> parent, View view,
                        int position, long id) {
                    if (cr.getRuleComparator(ruleIndex) == position) {
                        return;
                    }
                    cr.setRuleComparator(ruleIndex, position);
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
            valueEt.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        return;
                    }
                    cr.setRuleValue(ruleIndex, valueEt.getText().toString());
                }
            });
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    cr.deleteRule(ruleIndex);
                    setContentView(buildView());
                }
            });
            
            LinearLayout topRow = new LinearLayout(context);
            LinearLayout bottomRow = new LinearLayout(context);
            topRow.addView(colSpinner);
            topRow.addView(settingSpinner);
            bottomRow.addView(compSpinner);
            bottomRow.addView(valueEt);
            bottomRow.addView(deleteButton);
            LinearLayout rw = new LinearLayout(context);
            rw.setOrientation(LinearLayout.VERTICAL);
            rw.addView(topRow);
            rw.addView(bottomRow);
            return rw;
        }
        
        private Spinner getSpinner(Context context, String[] values) {
            Spinner spinner = new Spinner(context);
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(context,
                    android.R.layout.simple_spinner_item, values);
            adapter.setDropDownViewResource(
                    android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);
            return spinner;
        }
    }
}
