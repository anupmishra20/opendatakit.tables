package org.opendatakit.tables.fragments;

import static org.fest.assertions.api.ANDROID.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.common.android.data.UserTable;
import org.opendatakit.tables.activities.TableDisplayActivityStub;
import org.opendatakit.testutils.ODKFragmentTestUtil;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

@RunWith(RobolectricTestRunner.class)
public class SpreadsheetFragmentTest {
  
  SpreadsheetFragmentStub fragment;
  Activity activity;
  
  @Before
  public void setup() {
    ShadowLog.stream = System.out;
    // We don't want to build the menu here, as it doesn't
    // give us any information in this test class.
    TableDisplayActivityStub.BUILD_MENU_FRAGMENT = false;
  }
  
  private void doGlobalSetup() {
    this.fragment = new SpreadsheetFragmentStub();
    ODKFragmentTestUtil.startFragmentForTableActivity(
        TableDisplayActivityStub.class,
        this.fragment,
        null);
    this.activity = this.fragment.getActivity();
  }
  
  private void setupWithNoData() {
    // Make the UserTable show width 0.
    UserTable userTableMock = mock(UserTable.class);
    doReturn(0).when(userTableMock).getWidth();
    doGlobalSetup();
  }
  
  @After
  public void after() {
    TableDisplayActivityStub.resetState();
  }
  
  @Test
  public void buildViewWithWidthZeroIsTextView() {
    this.setupWithNoData();
    View fragmentView = this.fragment.getView();
    assertThat(fragmentView)
        .isNotNull()
        .isInstanceOf(TextView.class);
  }

}
