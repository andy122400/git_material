package pdm000;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.agile.api.APIException;
import com.agile.api.ChangeConstants;
import com.agile.api.IAgileList;
import com.agile.api.IAgileSession;
import com.agile.api.ICell;
import com.agile.api.IChange;
import com.agile.api.IDataObject;
import com.agile.api.IItem;
import com.agile.api.INode;
import com.agile.api.IRow;
import com.agile.api.IStatus;
import com.agile.api.ITable;
import com.agile.api.ITwoWayIterator;
import com.agile.api.IUser;
import com.agile.api.IWorkflow;
import com.agile.api.ItemConstants;
import com.agile.api.UserConstants;
import com.agile.px.ActionResult;
import com.agile.px.ICustomAction;
import com.andy.plm.accton.SqlFunction;
import com.andy.plm.accton.acctonRule;
import com.andy.plm.dblog.DBLogIt;
import com.andy.plm.html.HtmlFormat;

/****************************
 * Information*********************************。 Editor:Andy_chuang Last
 * Modify:2020 2020年8月17日 Description: program logic: 1.
 **********************************************************************/

public class Px_AutoApprover_check_ECN implements ICustomAction {
	
	
	public static void main(String[] args) throws APIException {
		Px_AutoApprover_check_ECN temp = new Px_AutoApprover_check_ECN();
		IAgileSession session = connector.agile.AgileSession_Connection.getAgileAdminSession();
		IDataObject obj = (IDataObject) session.getObject(IChange.OBJECT_TYPE, "ECN210100124");
		temp.doAction(session, null, obj);
		System.out.println(temp.sql_Error_Message);
	}

	/**********************************************************************
	 * 1.標準變數宣告(必要)
	 **********************************************************************/
	IAgileSession session;
	Connection msdb = connector.db.SqlServer_Connection.getMsDbConn("ERP-0-2");
	Connection joymsdb = connector.db.SqlServer_Connection.getMsDbConn("ERP-0-10");
	// IAgileSession adminSession = ConnectAll.getAgileAdminSession();//
	// 取得管理者最高權限
	// Connection msdb=ConnectAll.getProMsDbConn();
	// Connection oracledb=ConnectAll.getOracleDbConn();
	DBLogIt log = new DBLogIt();
	/**********************************************************************
	 * 2.其他全域變數宣告
	 **********************************************************************/
	private IChange currentchange;
	private IWorkflow currentworkflow;
	private IStatus currentStatus;
	private BufferedReader reader;
	private String ChangeWorkflow;
	private String ChangeStatus;
	private String ApproveType = "";
	private String Division = "";
	private String cellID;
	private String Condition_Filed;
	private String Condition_Value;
	private String Site_Filed;
	private String Site_Value;
	// private Integer ECNchange_PageThree_IssueType=2479246;
	private String user_logon;
	private Statement stmt;
	ArrayList<IUser> approverArrayList = new ArrayList<IUser>();
	ArrayList<IUser> observerArrayList = new ArrayList<IUser>();
	Set<String> tempset = new HashSet<String>(
			Arrays.asList(new String[] { "139", "152", "153", "249", "161", "165", "170", "175", "249" }));
	String sql_Error_Message = "";
	String html_header;
	Boolean Result_verification = true;
	String error = "";
	Set<String> set1 = new HashSet<String>();
	Set<String> set2 = new HashSet<String>();
	ResultSet rs;
	String current_Site_Value;
	private String pmdl_pn;
	private String project_name;
	private String pjm_type_name;
	private Set<String> setA = new HashSet<String>();
	private Set<String> setB = new HashSet<String>();

	/**********************************************************************
	 * 3.主程式(請勿修改位置)
	 **********************************************************************/
	@SuppressWarnings("finally")
	public ActionResult doAction(IAgileSession session, INode node, IDataObject obj) {
		/**********************************************************************
		 * 3-1.dblog參數設定
		 **********************************************************************/
		log.setUserInformation(session);
		log.setPxInfo(obj);
		log.setPgName("pdm000.Px_AutoApprover_check_ECN");// 自行定義程式名稱
		/**********************************************************************
		 * 3-2.doAction
		 **********************************************************************/
		this.session = session;
		try {
			int aqa = 123;
			stmt = msdb.createStatement();
			currentchange = (IChange) obj;
			if (currentchange.getCell(ChangeConstants.ATT_COVER_PAGE_WORKFLOW).getValue().toString().length() == 0) {
				error = "請先選擇表單流程";
				Result_verification = false;
				// session.createObject(aqa, arg1)
			} else {
				currentworkflow = currentchange.getWorkflow(); // 取的目前表單的Workflow名稱
				System.out.println("當前流程名稱: " + currentworkflow.getName());
				currentStatus = currentchange.getStatus();// 取的目前表單的站別名稱
				System.out.println("當前站別名稱: " + currentStatus.getName());
				getConfing("D:/Agile/Agile936/integration/sdk/extensions/Config_Approverby_WFECN.csv"); // 讀取設定檔，找到該表單流程暫別之參數檔
				readDataAndAddApprover();// ，讀取資料 加入簽核人員於List

				// System.out.println("approverArrayList====================================="
				// + approverArrayList.size());
				// System.out.println(approverArrayList.toString());
				//
				// addapprover();// 針對list執行人員加簽
				// removeAdmin();// 移除Admin管理員
				msdb.close(); // 關閉資料庫連線
				if (sql_Error_Message.equals("")) {
					sql_Error_Message = "檢查完畢，沒有錯誤";
				}
				html_header = HtmlFormat.Html_Header("ECN簽核前，Team Member 人員檢查程式", "WinSome(1573)", sql_Error_Message,
						obj);
			}
		} catch (Exception e) {
			log.setErrorMsg(e);

		} finally {
			log.updataDBLog();

			if (!Result_verification)
				return new ActionResult(ActionResult.EXCEPTION, new Exception(error));
			else
				return new ActionResult(ActionResult.STRING, html_header);

		}

	}

	/**********************************************************************
	 * 4.初始化
	 **********************************************************************/
	private void init() {
		// ini = new Ini();
	}

	/**********************************************************************
	 * 5.解構&釋放
	 **********************************************************************/
	private void close() {
		try {
			// adminSession.close();
		} catch (Exception e) {
		}
	}

	/**********************************************************************
	 * 6.以下為Biz Function
	 **********************************************************************/
	/**********************************************************************
	 * getConfing 取得Confing檔案
	 ***************************************************************/
	public void getConfing(String fileLocation) throws IOException, APIException, NumberFormatException, SQLException {
		InputStreamReader isr = new InputStreamReader(new FileInputStream(fileLocation));// 檔案讀取路徑
		reader = new BufferedReader(isr);
	}

	/**********************************************************************
	 * readDataAndAddApprover 讀取資料，並針對資料填件進行Approver List人員新增
	 * 
	 * @throws Exception
	 ***************************************************************/
	public void readDataAndAddApprover() throws Exception {
		String line = null;
		while ((line = reader.readLine()) != null) {
			String field[] = line.split(",");
			/** 讀取 **/
			// System.out.print(ChangeWorkflow + "\t" + ChangeStatus + "\t");
			ChangeWorkflow = field[1].trim();
			ChangeStatus = field[2].trim();
			// System.out.println("A:"+field[1].trim()+field[2].trim()+field[4].trim());
			// System.out.println("B:"+Division);
			// System.out.print(ChangeWorkflow + "\t" + ChangeStatus + "\t");
			// System.out.println(currentworkflow.getName());
			// System.out.println();
			// if
			// (((!Division.equals(field[4].trim())&&!ApproveType.equals(field[3].trim()))))
			// { // 同一Division只要符合一次即可
			if (ChangeWorkflow.equals(currentworkflow.getName())) {
				ApproveType = field[3].trim();
				Division = field[4].trim().replace("|", ",");
				cellID = field[5].trim();
				Condition_Filed = field[6].trim();
				Condition_Value = field[7].trim();
				String[] set1_array = Condition_Value.split(";");
				set1 = new HashSet<String>(Arrays.asList(set1_array));
				Site_Filed = field[8].trim();
				Site_Value = field[9].trim();
				System.out.println(ChangeWorkflow + "\t" + ChangeStatus + "\t" + ApproveType + "\t" + Division + "\t"
						+ Condition_Filed + "\t" + Site_Filed + "\t" + Site_Value);

				// break;

				switch (ApproveType) {
				case "1":
					getApproversByAffectedItemWhereUse_F();
					break;
				case "2":
					getObersversByAffectedItemWhereUse_F();
					break;
				// case "3":
				// getApproversByChangeCell();
				// break;
				case "6":
					getPMCApproversByAffectedItem_MFGSITE();
					break;

				case "7":
					getPMCObersersByAffectedItem_MFGSITE();
					break;
				// case "8":
				// getPAApproversByAffectedItem_MFGSITE();
				// break;
				// case "9":
				// getPAObersersByAffectedItem_MFGSITE();
				// break;
				}

			}

		}

		// }
	}

	private void getPAObersersByAffectedItem_MFGSITE() throws Exception {
		String current_Site_Value = currentchange.getCell(Integer.valueOf(Site_Filed)).getValue().toString();
		if (current_Site_Value.equals(Site_Value)) {
			if (currentchange.getCell(Integer.valueOf(Condition_Filed)).toString().contains(Condition_Value)) {
				ITable affectedtable = currentchange.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
				if (affectedtable.size() == 0)
					throw new Exception("受影響料號，不能為空");
				Iterator<?> it = affectedtable.iterator();
				String affectedItem = "";
				String childItem = "";
				while (it.hasNext()) {
					IRow row = (IRow) it.next();
					// String itemtype =
					// row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_ITEM_TYPE).toString();
					// affectedItem +=
					// row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_ITEM_NUMBER).getValue().toString()
					// + ",";

					IItem fatherItem = (IItem) row.getReferent();
					ITable RedlineTable = fatherItem.getTable(ItemConstants.TABLE_REDLINEBOM);
					ITwoWayIterator it2 = RedlineTable.getTableIterator();
					while (it2.hasNext()) {
						IRow redtablerow = (IRow) it2.next();
						if (redtablerow.isFlagSet(ItemConstants.FLAG_IS_REDLINE_MODIFIED)
								|| redtablerow.isFlagSet(ItemConstants.FLAG_IS_REDLINE_REMOVED)) {
							IItem item = (IItem) redtablerow.getReferent();
							if (acctonRule.getItemType(item).equals("Component")) {
								if (!isIgnoreParts(item)) {
									childItem += item + ",";
								}

							}

						}

					}

				}
				System.out.println(childItem);
				String Stm = "";

				if (Site_Value.equals("Accton"))
					Stm = ",'2'"; // 2為SP代號
				else if (Site_Value.equals("JoyTech"))
					Stm = ",'10'";// 10為SP代號
				else if (Site_Value.equals("All"))
					Stm = "";

				System.out.println("zp_agile_part_buyer '" + childItem + "'" + Stm);
				ResultSet rs = stmt.executeQuery("zp_agile_part_buyer '" + childItem + "'" + Stm);
				while (rs.next()) {
					user_logon = rs.getString(3);
					System.out.println(user_logon);
					IUser user = (IUser) session.getObject(UserConstants.CLASS_USERS_CLASS, user_logon);

					//
					if (!observerArrayList.contains(user))
						observerArrayList.add(user);

				}
			}
		}

	}

	private void getPAApproversByAffectedItem_MFGSITE() throws Exception {
		String current_Site_Value = currentchange.getCell(Integer.valueOf(Site_Filed)).getValue().toString();
		if (current_Site_Value.equals(Site_Value)) {
			if (currentchange.getCell(Integer.valueOf(Condition_Filed)).toString().contains(Condition_Value)) {
				ITable affectedtable = currentchange.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
				if (affectedtable.size() == 0)
					throw new Exception("受影響料號，不能為空");
				Iterator<?> it = affectedtable.iterator();
				String affectedItem = "";
				String childItem = "";
				while (it.hasNext()) {
					IRow row = (IRow) it.next();
					// String itemtype =
					// row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_ITEM_TYPE).toString();
					// affectedItem +=
					// row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_ITEM_NUMBER).getValue().toString()
					// + ",";

					IItem fatherItem = (IItem) row.getReferent();
					ITable RedlineTable = fatherItem.getTable(ItemConstants.TABLE_REDLINEBOM);
					ITwoWayIterator it2 = RedlineTable.getTableIterator();
					while (it2.hasNext()) {
						IRow redtablerow = (IRow) it2.next();
						if (redtablerow.isFlagSet(ItemConstants.FLAG_IS_REDLINE_MODIFIED)
								|| redtablerow.isFlagSet(ItemConstants.FLAG_IS_REDLINE_REMOVED)) {
							IItem item = (IItem) redtablerow.getReferent();
							if (acctonRule.getItemType(item).equals("Component")) {
								if (!isIgnoreParts(item)) {
									childItem += item + ",";
								}

							}

						}

					}

				}
				System.out.println(childItem);
				String Stm = "";

				if (Site_Value.equals("Accton"))
					Stm = ",'2'"; // 2為SP代號
				else if (Site_Value.equals("JoyTech"))
					Stm = ",'10'";// 10為SP代號
				else if (Site_Value.equals("All"))
					Stm = "";

				System.out.println("zp_agile_part_buyer '" + childItem + "'" + Stm);
				ResultSet rs = stmt.executeQuery("zp_agile_part_buyer '" + childItem + "'" + Stm);
				while (rs.next()) {
					user_logon = rs.getString(3);
					System.out.println(user_logon);
					IUser user = (IUser) session.getObject(UserConstants.CLASS_USERS_CLASS, user_logon);

					//
					if (!approverArrayList.contains(user))
						approverArrayList.add(user);

				}
			}
		}

	}

	/**********************************************************************
	 * Type6:getPMCApproversByAffectedItem_MFGSITE
	 * 
	 * @throws Exception
	 **********************************************************************/
	private void getPMCApproversByAffectedItem_MFGSITE() throws Exception {
		String current_Site_Value = currentchange.getCell(Integer.valueOf(Site_Filed)).getValue().toString();
		if (current_Site_Value.equals(Site_Value)) {
			// System.out.println("A");
			// System.out.println(Condition_Value);
			// if
			// (currentchange.getCell(Integer.valueOf(Condition_Filed)).toString().contains(Condition_Value))
			// {
			// System.out.println("B");
			String[] set2_array = currentchange.getCell(Integer.valueOf(Condition_Filed)).toString().split(";");
			set2 = new HashSet<String>(Arrays.asList(set2_array));
			if (hasIntersection(set1, set2)) {

				ITable affectedtable = currentchange.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
				if (affectedtable.size() == 0)
					throw new Exception("受影響料號，不能為空");
				Iterator<?> it = affectedtable.iterator();
				String affectedItem = "";
				while (it.hasNext()) {
					IRow row = (IRow) it.next();
					// String itemtype =
					// row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_ITEM_TYPE).toString();
					if (acctonRule.getAffectedItemType(row).equals("Phantom")) {
						// String itemtype =
						affectedItem = row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_ITEM_NUMBER).getValue().toString()
								+ ",";

						System.out.println(affectedItem);
						String Stm = "";
						System.out.println("exec pdmdb.dbo.zp_agile_get_fpartno  '" + affectedItem + "'");
						rs = stmt.executeQuery("exec pdmdb.dbo.zp_agile_get_fpartno '" + affectedItem + "'");
						affectedItem = "";
						while (rs.next()) {
							affectedItem = rs.getString(1);
							if (Site_Value.equals("Accton")) {
								Stm = ",'2'"; // 2為SP代號
								zp_pmc_Accton(Stm, affectedItem, approverArrayList);

							} else if (Site_Value.equals("JoyTech")) {
								Stm = ",'10'";// 10為SP代號
								zp_pmc_Joytech(Stm, affectedItem, approverArrayList);
							} else if (Site_Value.equals("All")) {
								Stm = "";
								zp_pmc_Accton(Stm, affectedItem, approverArrayList);
								zp_pmc_Joytech(Stm, affectedItem, approverArrayList);
							}
						}

					}
				}

				// System.out.println("zp_agile_product_planner '" +
				// affectedItem + "'" + Stm);
				// ResultSet rs = stmt.executeQuery("zp_agile_product_planner '"
				// + affectedItem + "'" + Stm);
				// while (rs.next()) {
				// user_logon = rs.getString(3);
				// System.out.println(user_logon);
				// IUser user = (IUser)
				// session.getObject(UserConstants.CLASS_USERS_CLASS,
				// user_logon);
				//
				// //
				// if (!approverArrayList.contains(user))
				// approverArrayList.add(user);
				//
				// }
			}
		}
	}

	/**********************************************************************
	 * Type7:getPMCApproversByAffectedItem_MFGSITE
	 * 
	 * @throws Exception
	 **********************************************************************/
	private void getPMCObersersByAffectedItem_MFGSITE() throws Exception {
		String current_Site_Value = currentchange.getCell(Integer.valueOf(Site_Filed)).getValue().toString();

		if (current_Site_Value.equals(Site_Value)) {
			// if
			// (currentchange.getCell(Integer.valueOf(Condition_Filed)).toString().contains(Condition_Value))
			// {
			String[] set2_array = currentchange.getCell(Integer.valueOf(Condition_Filed)).toString().split(";");
			set2 = new HashSet<String>(Arrays.asList(set2_array));
			if (hasIntersection(set1, set2)) {

				ITable affectedtable = currentchange.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
				if (affectedtable.size() == 0)
					throw new Exception("受影響料號，不能為空");
				Iterator<?> it = affectedtable.iterator();
				String affectedItem = "";
				while (it.hasNext()) {
					IRow row = (IRow) it.next();
					if (acctonRule.getAffectedItemType(row).equals("Phantom")) {
						// String itemtype =
						affectedItem = row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_ITEM_NUMBER).getValue().toString()
								+ ",";

						System.out.println(affectedItem);
						String Stm = "";
						System.out.println("exec pdmdb.dbo.zp_agile_get_fpartno  '" + affectedItem + "'");
						rs = stmt.executeQuery("exec pdmdb.dbo.zp_agile_get_fpartno '" + affectedItem + "'");
						affectedItem = "";
						while (rs.next()) {
							affectedItem = rs.getString(1);
							if (Site_Value.equals("Accton")) {
								Stm = ",'2'"; // 2為SP代號
								zp_pmc_Accton(Stm, affectedItem, observerArrayList);

							} else if (Site_Value.equals("JoyTech")) {
								Stm = ",'10'";// 10為SP代號
								zp_pmc_Joytech(Stm, affectedItem, observerArrayList);
							} else if (Site_Value.equals("All")) {
								Stm = "";
								zp_pmc_Accton(Stm, affectedItem, observerArrayList);
								zp_pmc_Joytech(Stm, affectedItem, observerArrayList);
							}
						}

					}
				}

				// System.out.println("zp_agile_product_planner '" +
				// affectedItem + "'" + B);
				// ResultSet rs = stmt.executeQuery("zp_agile_product_planner '"
				// + affectedItem + "'" + B);
				// while (rs.next()) {
				// user_logon = rs.getString(3);
				// System.out.println(user_logon);
				// IUser user = (IUser)
				// session.getObject(UserConstants.CLASS_USERS_CLASS,
				// user_logon);
				// if (!observerArrayList.contains(user))
				// observerArrayList.add(user);
				// //

				// }
			}
		}
	}

	/**********************************************************************
	 * Type1:getAffectedItemWhereUse_F_Approvers
	 * 
	 * @throws Exception
	 **********************************************************************/
	private void getApproversByAffectedItemWhereUse_F() throws Exception {
		if (!Site_Filed.equals("N/A")) {
			current_Site_Value = currentchange.getCell(Integer.valueOf(Site_Filed)).getValue().toString();
			if (current_Site_Value.equals(Site_Value)) {

				String[] set2_array = currentchange.getCell(Integer.valueOf(Condition_Filed)).toString().split(";");
				set2 = new HashSet<String>(Arrays.asList(set2_array));

				if (hasIntersection(set1, set2)) {
//					ITable affectedtable = currentchange.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
//					if (affectedtable.size() == 0)
//						throw new Exception("受影響料號，不能為空");
//					Iterator<?> it = affectedtable.iterator();
//					String affectedItem = "";
//					while (it.hasNext()) {
//						IRow row = (IRow) it.next();
//						IItem ie = (IItem) row.getReferent();
//						if (acctonRule.getAffectedItemType(row).equals("Phantom")) {
//							affectedItem += row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_ITEM_NUMBER).getValue()
//									.toString() + ",";
//						}
//					}
					
					Integer chnage_pagethree_FPartName = Integer.valueOf(1564);
					String Change_FPartName = currentchange.getCell(chnage_pagethree_FPartName).getValue().toString()
							.replace(";", ",");
//					rs = stmt.executeQuery(
//							"exec [PDMDB].[dbo].[zp_PMS_get_member] 1,'','','" + Change_FPartName + "','" + Division + "'");
//					String a = "exec [PDMDB].[dbo].[zp_PMS_get_member] 1,'','','" + Change_FPartName + "','" + Division
//							+ "'";
//					log.dblog("Division" + Division);
//					System.out.println("exec pdmdb.dbo.zp_agile_get_fpartno  '" + affectedItem + "'");
//					ResultSet rs = stmt.executeQuery("exec pdmdb.dbo.zp_agile_get_fpartno '" + affectedItem + "'");
//					affectedItem = "";
//					while (rs.next()) {
//						affectedItem += rs.getString(1).trim()+",";
////						System.out.println("exec [PDMDB].[dbo].[zp_PMS_get_member] 1,'','','" + affectedItem + "','"
////								+ Division + "'");
////						rs = stmt.executeQuery("exec [PDMDB].[dbo].[zp_PMS_get_member] 1,'','','" + affectedItem + "','"
////								+ Division + "'");
////
////						sql_Error_Message += SqlFunction.Sqlcheck_zp_PMS_get_member_Sql(session, affectedItem, rs, 0,
////								Division);
//					}
					rs = stmt.executeQuery("exec [PDMDB].[dbo].[zp_PMS_get_member] 1,'','','" + Change_FPartName + "','"
							+ Division + "'");
					sql_check(rs);

				}

			}

		} else {
			// System.out.println(currentchange.getCell(Integer.valueOf(Site_Filed)).getValue().toString());

			// if (current_Site_Value.equals(Site_Value)) {

			// String current_Site_Value =
			// currentchange.getCell(Integer.valueOf(Site_Filed)).getValue().toString();
			// System.out.println(current_Site_Value+")))))))))))))))))))))");
			String[] set2_array = currentchange.getCell(Integer.valueOf(Condition_Filed)).toString().split(";");
			set2 = new HashSet<String>(Arrays.asList(set2_array));

			if (hasIntersection(set1, set2)) {
				Integer chnage_pagethree_FPartName = Integer.valueOf(1564);
				String Change_FPartName = currentchange.getCell(chnage_pagethree_FPartName).getValue().toString()
						.replace(";", ",");
//				ITable affectedtable = currentchange.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
//				if (affectedtable.size() == 0)
//					throw new Exception("受影響料號，不能為空");
//				Iterator<?> it = affectedtable.iterator();
//				String affectedItem = "";
//				while (it.hasNext()) {
//					IRow row = (IRow) it.next();
//					IItem ie = (IItem) row.getReferent();
//					if (acctonRule.getAffectedItemType(row).equals("Phantom")) {
//						affectedItem += row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_ITEM_NUMBER).getValue()
//								.toString() + ",";
//					}
//				}
//				System.out.println("exec pdmdb.dbo.zp_agile_get_fpartno  '" + affectedItem + "'");
//				ResultSet rs = stmt.executeQuery("exec pdmdb.dbo.zp_agile_get_fpartno '" + affectedItem + "'");
//				affectedItem = "";
//				while (rs.next()) {
//					affectedItem += rs.getString(1).trim()+",";
////					System.out.println(
////							"exec [PDMDB].[dbo].[zp_PMS_get_member] 1,'','','" + affectedItem + "','" + Division + "'");
////					rs = stmt.executeQuery(
////							"exec [PDMDB].[dbo].[zp_PMS_get_member] 1,'','','" + affectedItem + "','" + Division + "'");
////
////					sql_Error_Message += SqlFunction.Sqlcheck_zp_PMS_get_member_Sql(session, affectedItem, rs, 0,
////							Division);
//				}
				rs = stmt.executeQuery(
						"exec [PDMDB].[dbo].[zp_PMS_get_member] 1,'','','" + Change_FPartName + "','" + Division + "'");
				sql_check(rs);

			}
		}
	}

	/**********************************************************************
	 * Type2:getAffectedItemWhereUse_F_Obersvers
	 * 
	 * @throws Exception
	 **********************************************************************/
	private void getObersversByAffectedItemWhereUse_F() throws Exception {
		if (!Site_Filed.equals("N/A")) {
			current_Site_Value = currentchange.getCell(Integer.valueOf(Site_Filed)).getValue().toString();

			log.dblog("current_Site_Value  : " + current_Site_Value);
			log.dblog("Site_Value   : " + Site_Value);
			if (current_Site_Value.equals(Site_Value)) {
				String[] set2_array = currentchange.getCell(Integer.valueOf(Condition_Filed)).toString().split(";");
				set2 = new HashSet<String>(Arrays.asList(set2_array));
				if (hasIntersection(set1, set2)) {
					Integer chnage_pagethree_FPartName = Integer.valueOf(1564);
					String Change_FPartName = currentchange.getCell(chnage_pagethree_FPartName).getValue().toString()
							.replace(";", ",");
//
//					ITable affectedtable = currentchange.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
//					if (affectedtable.size() == 0)
//						throw new Exception("受影響料號，不能為空");
//					Iterator<?> it = affectedtable.iterator();
//					String affectedItem = "";
//					while (it.hasNext()) {
//						IRow row = (IRow) it.next();
//						if (acctonRule.getAffectedItemType(row).equals("Phantom")) {
//							affectedItem += row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_ITEM_NUMBER).getValue()
//									.toString() + ",";
//						}
//
//					}
//					// System.out.println(affectedItem);
//					System.out.println("QQexec pdmdb.dbo.zp_agile_get_fpartno  '" + affectedItem + "'");
//					ResultSet rs = stmt.executeQuery("exec pdmdb.dbo.zp_agile_get_fpartno '" + affectedItem + "'");
//					affectedItem = "";
//					while (rs.next()) {
//						affectedItem += rs.getString(1).trim()+",";
////						System.out.println(affectedItem);
////						System.out.println("exec [PDMDB].[dbo].[zp_PMS_get_member] 1,'','','" + affectedItem + "','"
////								+ Division + "'");
////						rs = stmt.executeQuery("exec [PDMDB].[dbo].[zp_PMS_get_member] 1,'','','" + affectedItem + "','"
////								+ Division + "'");
////
////						sql_Error_Message += SqlFunction.Sqlcheck_zp_PMS_get_member_Sql(session, affectedItem, rs, 0,
////								Division);
//					}
					rs = stmt.executeQuery("exec [PDMDB].[dbo].[zp_PMS_get_member] 1,'','','" + Change_FPartName + "','"
							+ Division + "'");

					sql_check(rs);
				}
			}

		} else {
			String[] set2_array = currentchange.getCell(Integer.valueOf(Condition_Filed)).toString().split(";");
			set2 = new HashSet<String>(Arrays.asList(set2_array));
			if (hasIntersection(set1, set2)) {
				Integer chnage_pagethree_FPartName = Integer.valueOf(1564);
				String Change_FPartName = currentchange.getCell(chnage_pagethree_FPartName).getValue().toString()
						.replace(";", ",");
				rs = stmt.executeQuery("exec [PDMDB].[dbo].[zp_PMS_get_member] 1,'','','" + Change_FPartName + "','"
						+ Division + "'");

				sql_check(rs);
//
//				ITable affectedtable = currentchange.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
//				if (affectedtable.size() == 0)
//					throw new Exception("受影響料號，不能為空");
//				Iterator<?> it = affectedtable.iterator();
//				String affectedItem = "";
//				while (it.hasNext()) {
//					IRow row = (IRow) it.next();
//					if (acctonRule.getAffectedItemType(row).equals("Phantom")) {
//						affectedItem += row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_ITEM_NUMBER).getValue()
//								.toString() + ",";
//					}
//
//				}
//				// System.out.println(affectedItem);
//				System.out.println("exec pdmdb.dbo.zp_agile_get_fpartno  '" + affectedItem + "'");
//				ResultSet rs = stmt.executeQuery("exec pdmdb.dbo.zp_agile_get_fpartno '" + affectedItem + "'");
//				affectedItem = "";
//				while (rs.next()) {
//					affectedItem = rs.getString(1).trim();
//					System.out.println(affectedItem);
//					System.out.println("TTexec [PDMDB].[dbo].[zp_PMS_get_member] 1,'','','" + affectedItem + "','"
//							+ Division + "'");
//					rs = stmt.executeQuery(
//							"exec [PDMDB].[dbo].[zp_PMS_get_member] 1,'','','" + affectedItem + "','" + Division + "'");
//
//					sql_Error_Message += SqlFunction.Sqlcheck_zp_PMS_get_member_Sql(session, affectedItem, rs, 0,
//							Division);
//				}

			}
		}
	}

	/**********************************************************************
	 * getApproversByChange
	 **********************************************************************/
	private void getApproversByChangeCell() throws NumberFormatException, APIException {
		if (currentchange.getCell(Integer.valueOf(Condition_Filed)).toString().contains(Condition_Value)) {
			ICell cell = currentchange.getCell(Integer.valueOf(cellID));
			IAgileList AgileList = (IAgileList) cell.getValue();
			IAgileList[] AgileList_array = AgileList.getSelection();
			System.out.println(AgileList_array[0].getValue());
			for (IAgileList list : AgileList_array) {
				IUser users = (IUser) list.getValue();
				if (!approverArrayList.contains(users))
					approverArrayList.add(users);

			}

		}

	}

	/**********************************************************************
	 * addapprover
	 * 
	 **********************************************************************/
	public void addapprover() throws APIException {
		observerArrayList.removeAll(approverArrayList);// 去掉observer中與approver中重複之人員
		// System.out.println("AAAAAAAAAAAAAAAAAAAA"+approverArrayList);

		currentchange.addReviewers(currentStatus, approverArrayList, null, null, false, "");
	}

	/**********************************************************************
	 * removeAdmin
	 * 
	 **********************************************************************/
	@SuppressWarnings("deprecation")
	public void removeAdmin() throws APIException {

		IUser admin = (IUser) session.getObject(IUser.OBJECT_TYPE, "admin2");
		IUser[] approvers = new IUser[] { admin };

		currentchange.removeApprovers(currentStatus, approvers, null, "");

	}

	public boolean isIgnoreParts(IItem item) throws APIException {
		boolean results = false;
		String part_header = item.getCell(ItemConstants.ATT_TITLE_BLOCK_PART_TYPE).getValue().toString().split(" ")[0];
		System.out.println("part_header" + part_header);
		if (tempset.contains(part_header)) {
			System.out.println(item.getName() + "~於排除名單中");
			results = true;
		}
		return results;

	}

	public boolean hasIntersection(Set set1, Set set2) {

		int set1Size = set1.size();
		int set2Size = set2.size();
		set1.addAll(set2);
		int allSetSize = set1.size();

		if (allSetSize != (set1Size + set2Size))
			return true;
		else
			return false;

	}

	public String getCheckResult() {
		return sql_Error_Message;

	}

	public Boolean isCheckError() {

		return Result_verification;

	}

	public void zp_buyer_Accton(String Stm, String childItem, ArrayList<IUser> list) throws SQLException, APIException {
		System.out.println("zp_agile_part_buyer '" + childItem + "'" + Stm);
		rs = stmt.executeQuery("zp_agile_part_buyer '" + childItem + "'" + Stm);
		while (rs.next()) {
			user_logon = rs.getString(3);
			System.out.println(user_logon);
			IUser user = (IUser) session.getObject(UserConstants.CLASS_USERS_CLASS, user_logon);

			//
			if (!list.contains(user))
				list.add(user);

		}
	}

	public void zp_buyer_Joytech(String Stm, String childItem, ArrayList<IUser> list)
			throws SQLException, APIException {

		Statement joystmt = joymsdb.createStatement();
		String stm = "zp_agile_part_buyer '" + childItem + "'" + Stm;
		System.out.println(stm);
		rs = joystmt.executeQuery(stm);
		while (rs.next()) {
			user_logon = rs.getString(3);
			IUser user = (IUser) session.getObject(UserConstants.CLASS_USERS_CLASS, user_logon);
			if (!list.contains(user)) // 判斷不重複
				list.add(user);
		}

	}

	public void zp_pmc_Accton(String Stm, String childItem, ArrayList<IUser> list) throws SQLException, APIException {

		System.out.println("zp_agile_product_planner '" + childItem + "'" + Stm);
		System.out.println("*************************************************************");
		rs = stmt.executeQuery("zp_agile_product_planner '" + childItem + "'" + Stm);
		log.dblog("zp_agile_product_planner '" + childItem + "'" + Stm);
		sql_Error_Message += SqlFunction.Sqlcheck_zp_agile_product_planner_Sql(session, rs, childItem, "Accton");
		// while (rs.next()) {
		// user_logon = rs.getString(3);
		// System.out.println(user_logon);
		// IUser user = (IUser)
		// session.getObject(UserConstants.CLASS_USERS_CLASS, user_logon);
		// //
		// if (!list.contains(user))
		// list.add(user);
		// }
	}

	public void zp_pmc_Joytech(String Stm, String childItem, ArrayList<IUser> list) throws SQLException, APIException {
		String stm = "zp_agile_product_planner '" + childItem + "'" + Stm;
		log.dblog(Stm);
//		log.dblog(childItem);
//		log.dblog(list.toString());
//		log.dblog(stm.replace("'", "&"));
		Statement joystmt = joymsdb.createStatement();
		
		System.out.println(stm);
		rs = joystmt.executeQuery(stm);
		sql_Error_Message += SqlFunction.Sqlcheck_zp_agile_product_planner_Sql(session, rs, childItem, "Joytech");
		// while (rs.next()) {
		// user_logon = rs.getString(3);
		// IUser user = (IUser)
		// session.getObject(UserConstants.CLASS_USERS_CLASS, user_logon);
		// if (!list.contains(user)) // 判斷不重複
		// list.add(user);
		// }

	}
	public List<Map<String, Object>> selectAll(ResultSet rs) {
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		try {
			// 獲取結果集結構（元素據）
			ResultSetMetaData rmd = rs.getMetaData();
			// 獲取欄位數（即每條記錄有多少個欄位）
			int columnCount = rmd.getColumnCount();
			while (rs.next()) {
				// 儲存記錄中的每個<欄位名-欄位值>
				Map<String, Object> rowData = new HashMap<String, Object>();
				for (int i = 1; i <= columnCount; ++i) {
					// <欄位名-欄位值>
					rowData.put(rmd.getColumnName(i), rs.getObject(i));
				}
				// 獲取到了一條記錄，放入list
				list.add(rowData);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return list;
	}

	public void sql_check(ResultSet rs) throws APIException {
		List<Map<String, Object>> list = selectAll(rs);
		for (Map<String, Object> map : list) {
			 System.out.println(map.toString());
			user_logon = (String) map.get("user_logon"); // UserName
			pmdl_pn = (String) map.get("pmdl_pn"); // 料號
			project_name = (String) map.get("project_name"); // 專案
			pjm_type_name = (String) map.get("pjm_type_name");

			setB.add(pmdl_pn);
			if (user_logon == null) { // 如果Sql Table 維護null值時
				sql_Error_Message += "料號:[" + pmdl_pn + "]，請維護Team Member對應之[" + user_logon + "]人員" + "\n";
			} else {
				IUser user = (IUser) session.getObject(UserConstants.CLASS_USERS_CLASS, user_logon);
				if (user == null)
					sql_Error_Message += "料號:[" + pmdl_pn + "]" + "，Agile系統無對應之[" + user_logon + "]人員"+"-"+pjm_type_name + "\n";
			}
		}
		setA.removeAll(setB);
		if (setA.size() != 0) {
			sql_Error_Message += "料號:" + setA + "沒有維護[" + pjm_type_name + "]資料" + "\n";
		}
	}
}
