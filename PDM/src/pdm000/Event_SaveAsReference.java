package pdm000;

import com.agile.api.IAgileSession;
import com.agile.api.IItem;
import com.agile.api.INode;
import com.agile.api.ItemConstants;
import com.agile.px.ActionResult;
import com.agile.px.EventActionResult;
import com.agile.px.IEventAction;
import com.agile.px.IEventInfo;
import com.agile.px.ISaveAsEventInfo;
import com.andy.plm.accton.acctonRule;
import com.andy.plm.dblog.DBLogIt;

/**************************************************************
 * Editor: Last Modify: 2020年9月7日 Description:當Create時，自動將表單單號生成 program logic:
 * 1. 2. 3. 4.
 **********************************************************************/
public class Event_SaveAsReference implements IEventAction {
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
	Integer item_pagetwo_Ref_Component_Parts = 1279;
	Integer item_pagetwo_Ref_Part_Lifecycle = 1303;

	/**********************************************************************
	 * 3.主程式(請勿修改位置)
	 **********************************************************************/
	@SuppressWarnings("finally")
	public EventActionResult doAction(IAgileSession session, INode node, IEventInfo info) {
		/**********************************************************************
		 * 3-1.dblog參數設定
		 **********************************************************************/
		log.setUserInformation(session);
		log.setEventInfo(info);
		/**********************************************************************
		 * 3-2.doAction
		 **********************************************************************/
		try {
			ISaveAsEventInfo Info = (ISaveAsEventInfo) info;
			IItem item = (IItem) Info.getDataObject();
			System.out.println(item);
			if (acctonRule.getItemType(item).equals("Component")) {
				String ReferenceItemName = item.getCell(item_pagetwo_Ref_Component_Parts).getValue().toString();
				System.out.println(ReferenceItemName);
				// String ReferenceItemStatu =
				// item.getCell(1279).getValue().toString();
				if (isNull(ReferenceItemName)) { // 如果母料該欄位為空，表示為第一次進行SaveAs
					// String ItemName = item.getName().toString();
					String ItemStatus = item.getCell(ItemConstants.ATT_TITLE_BLOCK_LIFECYCLE_PHASE).toString();
					System.out.println("新" + Info.getNewNumber());
					IItem newItem = (IItem) session.getObject(IItem.OBJECT_TYPE, Info.getNewNumber());
					System.out.println("AAA" + newItem);
					// IItem item2 =session.getObject(IItem.OBJECT_TYPE,
					// Info.getNewNumber());
					newItem.getCell(item_pagetwo_Ref_Component_Parts).setValue(item);
					newItem.getCell(item_pagetwo_Ref_Part_Lifecycle).setValue(ItemStatus);
				}
			}else{
				log.dblog("為虛階料，程式不處理");
			}

			// newItem.getCell(1279).setValue("TEMP000000127");
			// newItem.getCell(12).setValue(ReferenceItemStatu);
			// else {// 如果母料該欄位不為空，表示為非初次進行SaveAs，其
			// IItem newItem = (IItem) session.getObject(IItem.OBJECT_TYPE,
			// Info.getNewNumber());
			// IItem ReferenceItem = (IItem)
			// session.getObject(IItem.OBJECT_TYPE, ReferenceItemName);
			// newItem.getCell(1279).setValue(ReferenceItem);
			// // newItem.getCell(12).setValue(ReferenceItemStatu);
			// }
		} catch (Exception e) {
			log.setErrorMsg(e);

		} finally {
			log.updataDBLog();
			return new EventActionResult(info, new ActionResult(ActionResult.STRING, log.getResult()));
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
	//
	// } catch (Exception e) {
	// }
	// }

	/**********************************************************************
	 * 6.以下為Biz Function
	 **********************************************************************/
	public Boolean isNull(String value) {
		if (value.equals("")) {
			return true;
		}
		return false;
	}
}