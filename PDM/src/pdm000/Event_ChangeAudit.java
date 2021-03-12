package pdm000;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.agile.api.APIException;
import com.agile.api.ChangeConstants;
import com.agile.api.IAgileSession;
import com.agile.api.ICell;
import com.agile.api.IChange;
import com.agile.api.INode;
import com.agile.api.IQualityChangeRequest;
import com.agile.api.IRow;
import com.agile.api.IStatus;
import com.agile.api.ITable;
import com.agile.api.IUser;
import com.agile.api.IWorkflow;
import com.agile.px.ActionResult;
import com.agile.px.EventActionResult;
import com.agile.px.IEventAction;
import com.agile.px.IEventInfo;
import com.agile.px.ISignOffEventInfo;
import com.andy.plm.dblog.DBLogIt;

/**************************************************************
 * Editor: Last Modify: 2020年10月13日 Description:當Create時，自動將表單單號生成 program
 * logic: 1. 2. 3. 4.
 **********************************************************************/
public class Event_ChangeAudit implements IEventAction {
	/**********************************************************************
	 * 1.標準變數宣告(必要)
	 **********************************************************************/
	// IAgileSession adminSession = ConnectAll.getAgileAdminSession();//
	// 取得管理者最高權限
	// Connection msdb=ConnectAll.getMsDbConn(); //ms資料庫連線
	// Connection oracledb=ConnectAll.getOracleDbConn(); //oracle資料庫連線
	DBLogIt log = new DBLogIt();

	/**********************************************************************
	 * 2.其他全域變數宣告
	 **********************************************************************/
	String userName;
	String ErrMsg = "";
	Map<?, ?> results;
	Boolean tag = true;

	/**********************************************************************
	 * 3.主程式(請勿修改位置)
	 **********************************************************************/
	public EventActionResult doAction(IAgileSession session, INode node, IEventInfo info) {
		/**********************************************************************
		 * 3-1.dblog參數設定
		 **********************************************************************/
		log.setUserInformation(session);
		log.setEventInfo(info);
		/**********************************************************************
		 * 3-2.doAction
		 **********************************************************************/
		ISignOffEventInfo Info = (ISignOffEventInfo) info;
		try {
			if (Info.getDataObject().getType() == IChange.OBJECT_TYPE) {
				IChange change = (IChange) Info.getDataObject();
				IWorkflow workflow = change.getWorkflow();
				IStatus status = change.getStatus();
				// IUser [] QQ =change.getApprovers(status);

//				if (!((workflow.getName().equals("PE Drawing Apply Workflow") && status.getName().equals("99.Released"))&&!isLastApprover(change))) {
					//					log.dblog("T_T");
//					if (isLastApprover(change)) {
//						log.dblog("QVQ");
						// change.audit(false);
						results = change.audit(false);
						log.dblog(results.toString());
						// System.out.println(results.toString());

						Set set = (Set) results.entrySet();
						Iterator it = ((Collection) set).iterator();
						while (it.hasNext()) {
							Map.Entry entry = (Map.Entry) it.next();
							ICell cell = (ICell) entry.getKey();
							if (cell != null) {
								System.out.println(change.getName() + "Cell : " + cell.getName());
							} else {
								// System.out.println(changeNum + "Cell : No
								// associated
								// data cell");
							}
							Iterator jt = ((Collection) entry.getValue()).iterator();
							while (jt.hasNext()) {
								APIException e = (APIException) jt.next();
								if (e.getMessage().contains("Affected Items.New Rev") == true
										|| e.getMessage().contains("Not all approvers responded") == true
										|| e.getMessage().contains("Not all acknowledgers responded") == true
										|| e.getMessage().contains("No errors or warnings") == true
										|| e.getMessage().contains(
												"Not all dependencies in the Relationships/Deliverables table have reached the trigger status.") == true) {
								} else {
									ErrMsg = ErrMsg + e.getMessage() + "\n";

									tag = false;
									System.out.println("Exception : " + change.getName() + ".Error:" + e.getMessage());
								}
							}
						}

//					}
				
				
				
				
//				if (workflow.getName().equals("PE Drawing Apply Workflow") && status.getName().equals("99.Released")) {
//					log.dblog("T_T");
//					if (isLastApprover(change)) {
//						log.dblog("QVQ");
//						// change.audit(false);
//						results = change.audit(false);
//						log.dblog(results.toString());
//						// System.out.println(results.toString());
//
//						Set set = (Set) results.entrySet();
//						Iterator it = ((Collection) set).iterator();
//						while (it.hasNext()) {
//							Map.Entry entry = (Map.Entry) it.next();
//							ICell cell = (ICell) entry.getKey();
//							if (cell != null) {
//								System.out.println(change.getName() + "Cell : " + cell.getName());
//							} else {
//								// System.out.println(changeNum + "Cell : No
//								// associated
//								// data cell");
//							}
//							Iterator jt = ((Collection) entry.getValue()).iterator();
//							while (jt.hasNext()) {
//								APIException e = (APIException) jt.next();
//								if (e.getMessage().contains("Affected Items.New Rev") == true
//										|| e.getMessage().contains("Not all approvers responded") == true
//										|| e.getMessage().contains("Not all acknowledgers responded") == true
//										|| e.getMessage().contains("No errors or warnings") == true
//										|| e.getMessage().contains(
//												"Not all dependencies in the Relationships/Deliverables table have reached the trigger status.") == true) {
//								} else {
//									ErrMsg = ErrMsg + e.getMessage() + "\n";
//
//									tag = false;
//									System.out.println("Exception : " + change.getName() + ".Error:" + e.getMessage());
//								}
//							}
//						}
//
//					}
//				} else {
//
//					// change.audit(false);
//					results = change.audit(false);
//					System.out.println(results.toString());
//
//					Set set = (Set) results.entrySet();
//					Iterator it = ((Collection) set).iterator();
//					while (it.hasNext()) {
//						Map.Entry entry = (Map.Entry) it.next();
//						ICell cell = (ICell) entry.getKey();
//						if (cell != null) {
//							System.out.println(change.getName() + "Cell : " + cell.getName());
//						} else {
//							// System.out.println(changeNum + "Cell : No
//							// associated
//							// data cell");
//						}
//						Iterator jt = ((Collection) entry.getValue()).iterator();
//						while (jt.hasNext()) {
//							APIException e = (APIException) jt.next();
//							if (e.getMessage().contains("Affected Items.New Rev") == true
//									|| e.getMessage().contains("Not all approvers responded") == true
//									|| e.getMessage().contains("Not all acknowledgers responded") == true
//									|| e.getMessage().contains("No errors or warnings") == true
//									|| e.getMessage().contains(
//											"Not all dependencies in the Relationships/Deliverables table have reached the trigger status.") == true) {
//							} else {
//								ErrMsg = ErrMsg + e.getMessage() + "\n";
//
//								tag = false;
//								System.out.println("Exception : " + change.getName() + ".Error:" + e.getMessage());
//							}
//						}
//					}
//					log.dblog("QVQ");
//				}
			} else {
				IQualityChangeRequest change = (IQualityChangeRequest) Info.getDataObject();
				results = change.audit(false);
				System.out.println(results.toString());

				Set set = (Set) results.entrySet();
				Iterator it = ((Collection) set).iterator();
				while (it.hasNext()) {
					Map.Entry entry = (Map.Entry) it.next();
					ICell cell = (ICell) entry.getKey();
					if (cell != null) {
						System.out.println(change.getName() + "Cell : " + cell.getName());
					} else {
						// System.out.println(changeNum + "Cell : No associated
						// data cell");
					}
					Iterator jt = ((Collection) entry.getValue()).iterator();
					while (jt.hasNext()) {
						APIException e = (APIException) jt.next();
						if (e.getMessage().contains("Affected Items.New Rev") == true
								|| e.getMessage().contains("Not all approvers responded") == true
								|| e.getMessage().contains("Not all acknowledgers responded") == true
								|| e.getMessage().contains("No errors or warnings") == true || e.getMessage().contains(
										"Not all dependencies in the Relationships/Deliverables table have reached the trigger status.") == true) {
						} else {
							ErrMsg = ErrMsg + e.getMessage() + "\n";

							tag = false;
							System.out.println("Exception : " + change.getName() + ".Error:" + e.getMessage());
						}
					}
				}

			}

		} catch (Exception e) {
			e.printStackTrace();
			log.setErrorMsg(e);

		}
		// log.updataDBLog();
		finally {
			log.updataDBLog();
			if (tag)
				return new EventActionResult(Info, new ActionResult(ActionResult.STRING, "finish"));
			else
				return new EventActionResult(Info, new ActionResult(ActionResult.EXCEPTION,
						new Exception("發生錯誤 Can't Approval.Please check below Message :" + "\n" + ErrMsg)));
		}

	}

	/**********************************************************************
	 * 4.初始化
	 **********************************************************************/
	// private void init() {
	//
	// }

	/**********************************************************************
	 * 5.解構&釋放
	 **********************************************************************/
	// private void close() {
	// try {
	// } catch (Exception e) {
	// }
	// }

	/**********************************************************************
	 * 6.以下為Biz Function
	 * 
	 * @throws APIException
	 **********************************************************************/
	public boolean isLastApprover(IChange change) throws APIException {
		IWorkflow workflow = change.getWorkflow();
		IStatus status = change.getStatus();
		IUser[] arrayAprrover = change.getApprovers(status);
		int AwaitingSize = 0;
		log.dblog(AwaitingSize + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
		log.dblog(arrayAprrover[0] + "" + arrayAprrover[1] + arrayAprrover[2] + "");
		// // System.out.println(change.getValue(1050));
		ITable table = change.getTable(ChangeConstants.TABLE_WORKFLOW);
		Iterator<?> it = table.iterator();
		int currentApproversSize = 0;
		while (it.hasNext()) {
			IRow cur_afrow = (IRow) it.next();
			// System.out.println(cur_afrow);
			// 3878 站別
			// 3875 最後簽核者
			// 3934 Current Process\
			// System.out.println(cur_afrow.getCell(3934).getValue().toString());
			log.dblog(cur_afrow.getCell(3878).getValue().toString());
			log.dblog(cur_afrow.getCell(3934).getValue().toString());
			log.dblog(cur_afrow.getCell(1111).getValue().toString());
			if (cur_afrow.getCell(3878).getValue().toString().equals(status.getName())
					&& cur_afrow.getCell(3934).getValue().toString().equals("Current Process")
					&& cur_afrow.getCell(1111).getValue().toString().equals("Awaiting Approval")) {
				AwaitingSize++;
			}
			// if
			// (cur_afrow.getCell(3878).getValue().toString().equals(status.getName())
			// && cur_afrow.getCell(3934).getValue().toString().equals("Current
			// Process")
			// &&
			// cur_afrow.getCell(1111).getValue().toString().equals("Approved"))
			// {
			// log.dblog("HI");
			// currentApproversSize++;
			// }
			// log.dblog(approversSize + "");
			// log.dblog(currentApproversSize + "");
			// }
			// approversSize = approversSize - 1; //

		}
		return AwaitingSize == 1;
	}
}