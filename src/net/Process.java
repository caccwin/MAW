package net;

import info.CardConfigInfo;
import info.CheckDeck;
import info.FairyInfo;
import info.FloorInfo;
import info.NoNameInfo;
import info.TimeManager;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

import start.GetConfig;
import start.Go;
import start.Info;
import start.Think;
import action.ActionRegistry.Action;
import action.AddFriends;
import action.BattleAreaBoss;
import action.ChangeCardItems;
import action.CheckFairyReward;
import action.FairyBattle;
import action.FairyHistory;
import action.FloorRun;
import action.GetFairyList;
import action.GetFloorInfo;
import action.GetAreaInfo;
import action.GetNoNameList;
import action.LevelUp;
import action.Login;
import action.PVPRedirect;
import action.PvpWithNoName;
import action.ReturnMain;
import action.RewardCheck;
import action.SellCard;
import autoBattle.AutoBattle;
import autoBattle.fairyAttackPredict;

@SuppressWarnings("unused")
public class Process {
	public static boolean noResponse = false;
	public static Connect connect;
	public static Info info;
	public static boolean update = false;
	public static int loop = 0;
	public Process() {
		System.out.print("初始化连接");
		connect = new Connect();
		System.out.println("[OK]");
		System.out.print("初始化数据");
		info = new Info();
		System.out.println("[OK]");
	}

	// byte转doc
	public static Document ParseXMLBytes(byte[] in) throws Exception {
		Info.errorPos = "ParseXMLBytes";
		ByteArrayInputStream bais = new ByteArrayInputStream(in);
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			return builder.parse(bais);
		} catch (Exception e) {
			throw e;
		}
	}

	public void start() {
		try {
			execute(Think.doIt(getPossibleAction()));
		} catch (Exception e) {
			if (e.getMessage() != null) {
				Go.log(e.getMessage());
				if (e.getMessage().contains("9000")) {
					try {
						Thread.sleep(1000 * (long)(Info.sleepTime +  Math.random() * 20));
					} catch (InterruptedException e1) {
						Go.log(e1.getMessage());
					} finally {
						info.events.clear();
						SellCard.tried = false;
						info.events.push(Info.EventType.cookieOutOfDate);
					}
				}else if (e.getMessage().contains("8000")){
					if(e.getMessage().contains("卡片"))
						try{
							info.events.clear();							
						}finally{
							info.events.push(Info.EventType.cardFull);
						}
				}else if (e.getMessage().contains("1010") && e.getMessage().contains("消灭") || e.getMessage().contains("逃跑")){
					info.canBattleFairyInfos = new ArrayList<FairyInfo>();
					info.events.push(Info.EventType.fairyAppear);
				}else if (e.getMessage().contains("103")){
					info.events.push(Info.EventType.notLoggedIn);
				}else if (e.getMessage().contains("维护") || e.getMessage().contains("官网")){
					try {
						Thread.sleep(1000 * (long)(10 * 60 +  Math.random() * 20));
					} catch (InterruptedException e1) {
						Go.log(e1.getMessage());
					} finally {
						info.events.clear();
						info.events.push(Info.EventType.cookieOutOfDate);
					}
				}				
			} else {
				if (info.events.isEmpty())
					info.events.push(Info.EventType.fairyAppear);
				else{
					info.events.clear();
					Go.log("服务器未响应" + e);
					if (!noResponse){
						noResponse = true;
						info.events.push(Info.EventType.fairyAppear);
						}
//					else {
//						noResponse = false;
//						info.events.push(Info.EventType.cookieOutOfDate);
//						}
				}
			}
		}
	}

	// 获取action方案
	private static List<Action> getPossibleAction() {
		ArrayList<Action> result = new ArrayList<Action>();
		if (info.events.size() != 0) {
			switch (info.events.pop()) {
			case notLoggedIn:
			case cookieOutOfDate:
				result.add(Action.LOGIN);
				break;
			case fairyCanBattle:
				result.add(Action.PRIVATE_FAIRY_BATTLE);
				break;
			case needFloorInfo:
				result.add(Action.GET_FLOOR_INFO);
				break;
			case fairyAppear:
				result.add(Action.GET_FAIRY_LIST);
				break;
			case needAPBCInfo:
				result.add(Action.GOTO_FLOOR);
				break;
			case levelUp:
				result.add(Action.LV_UP);
			case getNoNameList:
				result.add(Action.GET_NONAME_LIST);
				break;
			case pvp:
				result.add(Action.PVP);
				break;
			case fairyHistory:
				result.add(Action.FAIRY_HISTORY);
				break;
			case cardFull:
				result.add(Action.SELL_CARD);
				break;
			case addFriends:
				result.add(Action.ADD_FRIENDS);
				break;
			default:
			}
		}
		return result;
	}

	// 执行action
	private void execute(Action action) throws Exception {
		switch (action) {
		case LOGIN:
			Info.errorPos = "Process.Login";
			try {
				if (Login.run()) {
					if(update){
						System.out.println("");
						Go.log("保存当前版本号" + Info.userAgent);
						GetConfig.saveConfig(Info.userAgent , 1030);
						update = false;
					}
					if (info.country_id > 3 || info.country_id < 1)
						info.country_id = (int)( Math.random() * 3 + 0.5 );
					AutoBattle.Init(info.country_id);
					Go.log("cookie:" + info.cookie);					
					String str = ("登陆成功 用户:" + info.userName + " 等级:" + info.userLv
							+ " AP:" + info.apCurrent + "/" + info.apMax
							+ " BC:" + info.bcCurrent + "/" + info.bcMax
							+ " 卡:" + info.cardNum + "/" + info.cardMax							
							+ " 友情点:" + info.friendpoint);
					if (info.friends != info.friendMax && info.invitations > 0 )
						str = str + " 好友:" + info.friends + "(+"+ info.invitations + ")/" + info.friendMax;
					else
						str = str + " 好友:" + info.friends + "/" + info.friendMax;
					if (info.gather != 0)
						str = str + " 收集品:" + info.gather;
					Go.log(str);
//					if(loop == 0)
//					RewardCheck.run();
					if (!SellCard.tried && info.cardNum >= info.cardMax){
						Go.log("现有卡片 " + info.cardNum +"/"+ info.cardMax + " ,开始卖卡或合成");
						info.events.push(Info.EventType.cardFull);
						break;
						}
					info.events.push(Info.EventType.fairyAppear);
				} else {
					info.events.push(Info.EventType.notLoggedIn);
				}
			} catch (Exception ex) {
				System.out.println(ex.toString());
				info.events.push(Info.EventType.notLoggedIn);
			}
			CheckDeck.run();
			break;
		case GET_FAIRY_LIST:
			Info.errorPos = "Process.GET_FAIRY_LIST";
			if (info.nextExp <= 3 * info.apCurrent * 1.2){
				Info.hasPrivateFairyStopRun = 0;
			}
			if (info.freeApBcPoint > 0) {
				info.events.push(Info.EventType.levelUp);
				break;
			}
			if (GetFairyList.run()) {
				info.events.push(Info.EventType.fairyHistory);
				break;
			} else {
				ReturnMain.run();
				// 如果ap大于当前地图所需cost则开始跑图
				if (info.apCurrent >= info.floorCost && Info.isRun.equals("1")
						&& info.bcCurrent < Info.stopRunWhenBcMore
						&& info.apCurrent > Info.stopRunWhenApLess
						&& Info.canRun == 1 || info.apCurrent > info.apMax - 10) {
					Go.log("开始跑图");
					info.events.push(Info.EventType.needFloorInfo);
					break;
				}
				// BC如果大于设定值则开始点名
				if ((info.bcCurrent >= Info.whenBcMoreThan)
						&& Info.isPVP.equals("1")) {
					Go.log("当前BC大于" + Info.whenBcMoreThan + " 进行点名操作");
					info.events.push(Info.EventType.getNoNameList);
					break;
				}
/**				if(loop % 140 == 0)
					RewardCheck.run();
				if (loop < Integer.MAX_VALUE -10 )
				loop ++; **/
				if (info.friends != info.friendMax && info.invitations > 0 ){
					info.events.push(Info.EventType.addFriends);
					break;
				}
				if(CheckFairyReward.run()){
					info.events.push(Info.EventType.fairyAppear);
					break;
				}
				double wait = Info.waitTime + Info.timepoverty + 2
						* Math.random();
				if (wait > 300)
					wait = 300;				
				Thread.sleep((long) (1000 * wait));				
				if (Info.timepoverty < Info.timepovertyMAX && Info.timepoverty!=0)
					Info.timepoverty = (0.9 + Math.random()*0.6) * Info.timepoverty;
				else{
					Info.timepoverty = (0.8 + Math.random()*0.3) * Info.timepoverty;
				}
				info.events.push(Info.EventType.fairyAppear);
				Go.log(wait,true);
			}
			break;
		case GET_NONAME_LIST:
			Info.errorPos = "Process.GET_NONAME_LIST";
			if (info.freeApBcPoint > 0) {
				info.events.push(Info.EventType.levelUp);
				break;
			}
			if (GetNoNameList.run()) {
				Go.log("寻找无名亚瑟完毕");
				info.events.push(Info.EventType.pvp);
			}
			break;
		case PVP:
			Info.errorPos = "Process.PVP";
			if (info.nextExp <= 3 * info.apCurrent * 1.2){
				Info.hasPrivateFairyStopRun = 0;
			}
			try{
				ChangeCardItems.run(Info.pvpCard, Info.pvpLr);
			}catch(Exception e){
				if (e.toString().contains("8000") && e.toString().contains("储存失败")){
					Info.pvpCard = Info.wolf;
					Info.pvpLr = Info.wolfLr;
					}
			}			
			for (NoNameInfo noName : info.noNameList) {
				// 如果ap大于当前地图所需cost则开始跑图
				if (info.apCurrent >= info.floorCost && Info.isRun.equals("1")
						&& info.bcCurrent < Info.stopRunWhenBcMore
						&& info.apCurrent > Info.stopRunWhenApLess
						&& Info.canRun == 1 || info.apCurrent > info.apMax - 10) {
					Go.log("开始跑图");
					info.events.push(Info.EventType.needFloorInfo);
					break;
				}
				// 如果BC不足设定值，则留着BC舔妖精
				if (info.bcCurrent < Info.whenBcMoreThan) {
					Go.log("当前BC不足" + Info.whenBcMoreThan + " 停止点名");
					info.events.push(Info.EventType.fairyAppear);
					break;
				}
				if (PvpWithNoName.run(noName)) {
					if (info.isLvUp) {
						Go.log("升级了！");
						String str = ("战斗结果:" + info.battleResult
								+ " 金币:" + info.gold + " AP:"
								+ info.apCurrent + "/" + info.apMax + " BC:"
								+ info.bcCurrent + "/" + info.bcMax);
						if (info.gather != 0)
							str = str + " 收集品:" + info.gather;
						Go.log(str);
						info.events.push(Info.EventType.levelUp);
						break;
					} else {
						String str = ("战斗结果:" + info.battleResult + " 经验:"
								+ info.exp + "/" + info.nextExp + " 金币:" + info.gold + " AP:"
								+ info.apCurrent + "/" + info.apMax + " BC:"
								+ info.bcCurrent + "/" + info.bcMax);
						if (info.gather !=0)
							str = str + " 收集品:" + info.gather;
						Go.log(str);
					}
				}
				if (PVPRedirect.run()) {
					Go.log("刷新对战信息");
				}
				Go.log("等待战斗CD……");
				Thread.sleep((long) (1000 * (15 + Math.random() * 2)));
				// 每点名一个人就检测一下是否有妖精出现
				GetFairyList.run();
				for (FairyInfo fairyInfo : info.fairyInfos) {
					FairyHistory.run(fairyInfo);
				}
				if (info.canBattleFairyInfos.size() > 0) {
					info.events.push(Info.EventType.fairyCanBattle);
					break;
				}
				info.events.push(Info.EventType.getNoNameList);
			}
			break;
		case FAIRY_HISTORY:
			Info.errorPos = "Process.FAIRY_HISTORY";
			if (info.nextExp <= 3 * info.apCurrent * 1.2){
				Info.hasPrivateFairyStopRun = 0;
			}
			for (FairyInfo fairyInfo : info.fairyInfos) {
				FairyHistory.run(fairyInfo);
			}
			ReturnMain.run();
			if (info.canBattleFairyInfos.size() > 0) {
				info.events.push(Info.EventType.fairyCanBattle);
				break;
			} else {
				// 如果ap大于当前地图所需cost则开始跑图
				if (info.apCurrent >= info.floorCost && Info.isRun.equals("1")
						&& info.bcCurrent < Info.stopRunWhenBcMore
						&& info.apCurrent > Info.stopRunWhenApLess
						&& Info.canRun == 1 || info.apCurrent > info.apMax - 10) {
					Go.log("开始跑图");
					info.events.push(Info.EventType.needFloorInfo);
					break;
				}
				// BC如果大于设定值则开始点名
				if ((info.bcCurrent >= Info.whenBcMoreThan)
						&& Info.isPVP.equals("1")) {
					Go.log("当前BC大于" + Info.whenBcMoreThan + " 进行点名操作");
					info.events.push(Info.EventType.getNoNameList);
					break;
				}
				
				if (info.friends != info.friendMax && info.invitations > 0 ){
					info.events.push(Info.EventType.addFriends);
					break;
				}
				
				if(CheckFairyReward.run()){
					info.events.push(Info.EventType.fairyAppear);
					break;
				}
				
				double wait = Info.waitTime + Info.timepoverty + 2
						* Math.random();
				if (wait > 300)
					wait = 300;				
				Thread.sleep((long) (1000 * wait));
				if (Info.timepoverty < Info.timepovertyMAX  && Info.timepoverty!=0)
					Info.timepoverty = (0.9 + Math.random()*0.6) * Info.timepoverty;
				else{
					Info.timepoverty = (0.8 + Math.random()*0.3) * Info.timepoverty;
				}
				info.events.push(Info.EventType.fairyAppear);
				Go.log(wait,true);
				break;
			}
		case PRIVATE_FAIRY_BATTLE:
			Info.errorPos = "Process.PRIVATE_FAIRY_BATTLE";
			if (!SellCard.tried && info.cardNum >= info.cardMax){
				Go.log("现有卡片 " + info.cardNum +"/"+ info.cardMax + " ,开始卖卡或合成");
				if(SellCard.run()){
					Go.log("卖卡成功");
					ReturnMain.run();
					}
				else{
					Go.log("卖卡失败，等待用户");
					info.events.push(Info.EventType.fairyAppear);
					break;
					}
				}
			Info.errorPos += "1";
			if (Info.timepoverty > 2 && Info.timepoverty < 20)
				Info.timepoverty = Info.timepoverty
						/ info.canBattleFairyInfos.size() + 1 + Math.random() * 10;
			else{
				if(Info.timepoverty!=0)
				Info.timepoverty = 8;
			}
			Info.errorPos += "2";
			if (info.canBattleFairyInfos.size() > 20){
				Go.log("发现异常");
				info.canBattleFairyInfos = new ArrayList<FairyInfo>();
				info.events.push(Info.EventType.fairyAppear);
				break;
				}
			Info.errorPos += "3";
			String msg2 = "";
			if (info.canBattleFairyInfos.size() > 0)
				msg2 =("发现妖精！共计" + info.canBattleFairyInfos.size() + "只");
			if (info.fairyRewardCount > 0)
				msg2 = msg2 + (" 未领奖励:" + info.fairyRewardCount);
			Go.log(msg2);
			for (FairyInfo fairyInfo : info.canBattleFairyInfos) {
				if (info.bcCurrent < 2) {
					Go.log("当前BC" + (Info.lickCost - info.bcCurrent) + "，等待回复……");
					Thread.sleep(1000 * 60 * (Info.lickCost - info.bcCurrent));
				}
				if (!SellCard.tried && info.cardNum > info.cardMax){
					Go.log("现有卡片 " + info.cardNum +"/"+ info.cardMax + " ,开始卖卡或合成");
					if(SellCard.run()){
						Go.log("卖卡成功");
						ReturnMain.run();
						}
					else{
						Go.log("卖卡失败，等待用户");
						break;
					}
					}
				int fairyatk = fairyAttackPredict.Predict(fairyInfo);
				Go.log("用户:" + fairyInfo.userName + " 妖精:" + fairyInfo.name
						+ " 等级:" + fairyInfo.lv + " HP:" + fairyInfo.currentHp
						+ "/" + fairyInfo.maxHp + "["+ fairyatk +"]");
				Info.errorPos += "4[" + fairyInfo.type +"]";
				if (Info.fairyType.contains(fairyInfo.type + "")){
					Info.errorPos += "40";
					int fairyLv = Integer.parseInt(fairyInfo.lv);
					Info.errorPos += "41";
					int type = 0;
					if (URLEncoder.encode(fairyInfo.name, "utf-8").contains("%E7%9A%84"))
						type = 1;
					Info.errorPos += "42";
					String Deck = AutoBattle.GetWinDeck(fairyLv, fairyInfo.maxHp, fairyInfo.currentHp, fairyatk, info.bcCurrent, info.ex_gauge);
					if (Deck.equals("null"))
						Deck = AutoBattle.GetCollectionDeck(fairyLv, type, fairyInfo.maxHp, fairyInfo.currentHp, fairyatk, info.bcCurrent, Info.PreferInt, info.ex_gauge);
					if (Deck.equals("null"))
						ChangeCardItems.run(Info.wolf, Info.wolfLr);
					else
						ChangeCardItems.run(Deck, Deck.substring(0,Deck.indexOf(",")));
					Info.errorPos += "43";
					if (!Deck.equals("null"))
						Go.log("切换卡组:" + Process.info.CurrentDeck);
					else Go.log("切换卡组:狼娘");
					Info.errorPos += "45";
				} else {
					Info.errorPos += "46";
					ChangeCardItems.run(Info.wolf, Info.wolfLr);
					Go.log("切换卡组:狼娘");
				}
				int beforeBC = info.bcCurrent;
				if (FairyBattle.run(fairyInfo)) {
					if (info.isLvUp) {
						String str = ("战斗结果:" + info.battleResult
								+ " 金币:" + info.gold + " AP:"
								+ info.apCurrent + "/" + info.apMax + " BC:"
								+ info.bcCurrent + "(-" + (beforeBC - info.bcCurrent) + ")/" + info.bcMax 
								+ " EX:" + info.ex_gauge);
						if (info.gather != 0 || info.GuildGather != 0){
							if(fairyInfo.race_type.equals("12"))
								str = str + " 公会收集品:" + info.GuildGather + "(+" + info.GetGather + ")";
							else
								str = str + " 收集品:" + info.gather + "(+" + info.GetGather + ")";
							}
						Go.log(str);
						Go.log("升级了！");
						if(!TimeManager.found || TimeManager.done)
							Info.hasPrivateFairyStopRun = Info.hasPrivateFairyStopRunOriginal;
						info.events.push(Info.EventType.levelUp);
						break;
					} else {
						String str = ("战斗结果:" + info.battleResult + " 经验:"
								+ info.exp + "/" + info.nextExp + " 金币:" + info.gold + " AP:"
								+ info.apCurrent + "/" + info.apMax + " BC:"
								+ info.bcCurrent + "(-" + (beforeBC - info.bcCurrent) + ")/" + info.bcMax 
								+ " EX:" + info.ex_gauge);
						if (info.gather != 0 || info.GuildGather != 0){
							if(fairyInfo.race_type.equals("12"))
								str = str + " 公会收集品:" + info.GuildGather + "(+" + info.GetGather + ")";
							else
								str = str + " 收集品:" + info.gather + "(+" + info.GetGather + ")";
							}
						Go.log(str);
					}
				}
				// 如果BC不足2，则等待两分钟			
				Go.log("等待战斗CD……");
				Thread.sleep((long) (1000 * (15 + Math.random() * 2)));
				
			}
			
			// 重置
			info.canBattleFairyInfos = new ArrayList<FairyInfo>();
			info.events.push(Info.EventType.fairyAppear);
			break;
		case LV_UP:
			Info.errorPos = "Process.LV_UP";
			if (LevelUp.run()) {
				Process.info.isLvUp = false;
				Process.info.freeApBcPoint = 0;
				Info.hasPrivateFairyStopRun = Info.hasPrivateFairyStopRunOriginal;
				Go.log("加点完毕");
				Go.log(info.userName + " 等级:" + info.userLv + " AP:"
						+ info.apCurrent + "/" + info.apMax + " BC:"
						+ info.bcCurrent + "/" + info.bcMax + " 剩余:"
						+ info.freeApBcPoint);
			}
			info.events.push(Info.EventType.fairyAppear);
			break;
		case GET_FLOOR_INFO:
			Info.errorPos = "Process.GET_FLOOR_INFO";
			if (!SellCard.tried && info.cardNum >= info.cardMax){
				Go.log("现有卡片 " + info.cardNum +"/"+ info.cardMax + " ,开始卖卡或合成");
				if(SellCard.run()){
					Go.log("卖卡成功");
					ReturnMain.run();
					}
				else{
					Go.log("卖卡失败，等待用户");
					info.events.push(Info.EventType.fairyAppear);
					break;
					}
			}
			Info.errorPos += "1";
			if (GetAreaInfo.run(false)) {
				Info.errorPos += "10";
				FloorInfo floorInfo = null;
				boolean isClear = false;
				if (info.floorInfos.size() > 0) {
					if (Info.dayFirst.equals("1") && Info.runFactor.equals("1")
							|| Info.maptimelimitUp != -1) {
						for (FloorInfo fInfo : info.floorInfos) {
							if (Info.maptimelimitUp != -1) {
								Info.errorPos += "11";
								if (fInfo.name.contains("奖励")) {
									Go.log("限时秘境完成");
									floorInfo = fInfo;
									TimeManager.done = true;
									break;
								}
								if (fInfo.name.contains("限时")) {
									Go.log("优先限时秘境");
									Info.hasPrivateFairyStopRun = 0;
									floorInfo = fInfo;
									Info.maptimelimit = true;
									TimeManager.found = true;
									break;
								}
							}
							if (Info.dayFirst.equals("1")
									&& fInfo.id.contains("5000")) {
								Info.errorPos += "12";
								floorInfo = fInfo;
								break;
							}

							else {
								if (!Process.info.hasPartyFairy
										&& fInfo.race_type.equals("12")
										&& Info.canRun == 0) {
									floorInfo = fInfo;
									Info.errorPos += "13";
								}
								if (!fInfo.race_type.equals("12")
										&& !fInfo.id.contains("5000")) {
									floorInfo = fInfo;
									Info.errorPos += "14";
								}
							}
							floorInfo = fInfo;
							Info.errorPos += "15";
						}
						GetFloorInfo.run(floorInfo, false);
						Info.errorPos += "16";
					} else {
						if (Info.whatMap == 0) {
							Info.errorPos += "21";
							GetAreaInfo.run(true);
							for (FloorInfo fInfo : info.floorInfos) {
								if (!Process.info.hasPartyFairy
										&& fInfo.race_type.equals("12")
										&& Info.canRun == 0) {
									floorInfo = fInfo;
									break;
								}
								if (!fInfo.race_type.equals("12")
										&& !fInfo.id.contains("5000")) {
									floorInfo = fInfo;
									break;
								}
								floorInfo = fInfo;
							}
							Info.errorPos += "22";
							GetFloorInfo.run(floorInfo, false);							
						} else {
							floorInfo = info.floorInfos.get(Info.whatMap);
							Info.errorPos += "23";
							GetFloorInfo.run(floorInfo, true);
						}
					}
				} else {
					Info.errorPos += "31";
					GetAreaInfo.run(true);
					if (Info.whatMap == 0) {
						for (FloorInfo fInfo : info.floorInfos) {
							if (!Process.info.hasPartyFairy
									&& fInfo.race_type.equals("12")
									&& Info.canRun == 0) {
								floorInfo = fInfo;
								break;
							}
							if (!fInfo.race_type.equals("12")
									&& !fInfo.id.contains("5000")) {
								floorInfo = fInfo;
								break;
							}
							floorInfo = fInfo;
						}
					} else {
						floorInfo = info.floorInfos.get(Info.whatMap);
					}
					GetFloorInfo.run(floorInfo, true);
					isClear = true;
				}	
				Go.log("获取地图完毕,探索:" + floorInfo.name);
				if (!info.floorId.equals("") && info.floorCost != 0) {
					if (info.apCurrent < info.floorCost) {
						Go.log("AP不足" + info.floorCost);
						info.events.push(Info.EventType.fairyAppear);
						break;
					} if (info.apCurrent < Info.stopRunWhenApLess + info.floorCost && !Info.maptimelimit){
						info.floorCost += Info.stopRunWhenApLess;
						Go.log("AP不足" + Info.stopRunWhenApLess);
						info.events.push(Info.EventType.fairyAppear);
						break;
						}
					else {
						while (FloorRun.run(floorInfo)) {
							String msg = (floorInfo.name+" 第"+info.floorId+"层"+info.progress+"% 消耗AP"+info.floorCost
									+" 经验:"+info.getExp + "/" + info.nextExp 
									+" 金币:"+info.runGold );

							if (!info.event_type.equals("0")){
								if (info.event_type.equals("获得道具"))
									msg += (" 收集品:" + info.gather);
								else if (info.event_type.equals("遇到小伙伴"))
									msg += (" 友情点:" + info.friendpoint);
								else
									msg += (" 事件:"+info.event_type);								
							}
							msg += 	(" AP:"+info.apCurrent+ "/" +info.apMax 
									+" BC:"+info.bcCurrent + "/" + info.bcMax);
							Go.log(msg);
							if(floorInfo.name.contains("限时")){
								Info.hasPrivateFairyStopRun = 0;
								Info.maptimelimit = true;
								TimeManager.found = true;
							}
							if (info.nextExp <= info.getExp * info.apCurrent / info.floorCost * 1.2){
								Info.hasPrivateFairyStopRun = 0;
							}
							// 地图踏破则更新楼层
							if (info.progress == 100 && !isClear) {
								info.floorId = info.nextFloorId;
								info.floorCost = info.nextFloorCost;
							}
							// 每跑一个图就检测一下是否有妖精出现
							GetFairyList.run();
							for (FairyInfo fairyInfo : info.fairyInfos) {
								FairyHistory.run(fairyInfo);
							}
							if (info.canBattleFairyInfos.size() > 0) {
								info.events.push(Info.EventType.fairyCanBattle);
								break;
							}
							// 地图clear则重新获取地图
							if (info.areaClear == 1 && !isClear) {
								info.events.push(Info.EventType.needFloorInfo);
								if (Info.maptimelimit){
									Info.hasPrivateFairyStopRun = 1;
								}
								break;
							}
							// ap不足则跳出循环
							if (info.apCurrent < info.floorCost) {
								Go.log("AP不足" + info.floorCost);
								info.events.push(Info.EventType.fairyAppear);
								break;
							}
							if (info.apCurrent < Info.stopRunWhenApLess + info.floorCost && !Info.maptimelimit){
								info.floorCost += Info.stopRunWhenApLess;
								Go.log("AP不足" + info.floorCost);
								info.events.push(Info.EventType.fairyAppear);
								break;
							}
							// bc超过设定值则跳出循环
							if (info.bcCurrent > Info.stopRunWhenBcMore) {
								Go.log("BC超过设定值，停止跑图");
								info.events.push(Info.EventType.fairyAppear);
								break;
							}
							if (info.apCurrent < Info.stopRunWhenApLess) {
								Go.log("AP低于设定值，停止跑图");
								info.events.push(Info.EventType.fairyAppear);
								break;
							}
							if (!(info.apCurrent >= info.floorCost && Info.isRun.equals("1")
									&& info.bcCurrent < Info.stopRunWhenBcMore
									&& info.apCurrent > Info.stopRunWhenApLess
									&& Info.canRun == 1 || info.apCurrent > info.apMax - 10)){
								Go.log("停止跑图");
								info.events.push(Info.EventType.fairyAppear);
								break;
							}
							if (!SellCard.tried && info.cardNum >= info.cardMax){
								Go.log("现有卡片 " + info.cardNum +"/"+ info.cardMax + " ,开始卖卡或合成");
								if(SellCard.run()){
									Go.log("卖卡成功");
									ReturnMain.run();
									}					
								else{
									Go.log("卖卡失败，等待用户");
									info.events.push(Info.EventType.fairyAppear);
									break;
									}
								}
							Thread.sleep((long) (1000 * Math.random()));
						}
					}
				} else {
					if (info.bossId != 0) {
						Go.log("秘境守护者出现！");
						try{
							ChangeCardItems.run(Info.battleBoss, Info.battleBossLr);
						}catch(Exception e){
							if (e.toString().contains("8000") && e.toString().contains("储存失败")){
								Info.battleBoss = Info.wolf;
								Info.battleBossLr = Info.wolfLr;
								}
						}
						if (BattleAreaBoss.run(floorInfo)) {
						info.events.push(Info.EventType.needFloorInfo);
						break;
						}
						
					}
			}
			} else {
				info.events.push(Info.EventType.fairyAppear);
			}
			break;
		case SELL_CARD:
			Info.errorPos = "Process.SELL_CARD";
			try {
				Go.log("尝试卖卡");
				if(SellCard.run()){
					Go.log("卖卡成功");
					ReturnMain.run();
					info.events.push(Info.EventType.fairyAppear);
					break;
					}
				else
					Go.log("等待用户");
			}
			catch (Exception ex){
				throw ex;
			}
			info.events.push(Info.EventType.fairyAppear);
			break;
		case ADD_FRIENDS:
			Info.errorPos = "Process.ADD_FRIENDS";
			try {
				Go.log("获取好友列表");
				if(AddFriends.run()){
					Go.log("获取好友");
					ReturnMain.run();
					info.events.push(Info.EventType.fairyAppear);
					break;
					}
				else
					Go.log("等待用户");
			}
			catch (Exception ex){
				throw ex;
			}
			info.events.push(Info.EventType.fairyAppear);
			break;
		default:
			Info.errorPos = "Process.default";
			Thread.sleep((long) (( 10 * Math.random() ) * 1000));
			info.events.push(Info.EventType.fairyAppear);
		}
	}

}
