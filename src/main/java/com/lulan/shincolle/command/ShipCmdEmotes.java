package com.lulan.shincolle.command;

import com.lulan.shincolle.network.S2CSpawnParticle;
import com.lulan.shincolle.proxy.CommonProxy;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.CommandBlockBaseLogic;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;

import javax.annotation.Nullable;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Command: /shipemotes
 * <p>
 * show emotes
 * <p>
 * authority: all player
 * <p>
 * type:
 * 1. /shipemotes [emotes]
 * if no parm, show random emotes
 */
public class ShipCmdEmotes extends CommandBase {

    public static class EmoteData {
        private static final Map<String, Integer> nameToIdMap;
        private static final String[] nameArray;
        private static final Map<String, Integer> emotionPriorityMap; // 감정 표현의 우선순위

        static {
            // 이모트 데이터 초기화 (다국어 감정 표현 포함)
            Map<Integer, List<String>> idToNames = new LinkedHashMap<>() {{
                put(0, Arrays.asList("0", "swt", "drop", "당황", "어쩌지", "困った", "어떡해"));

                put(1, Arrays.asList("1", "lv", "love", "heart",
                        "사랑", "좋아", "러브", "♥", "❤", "💕", "💖", "💗", "💘", "💙", "💚", "💛", "💜", "🖤", "🤍", "🤎",
                        "愛", "好き", "ラブ", "恋", "爱", "喜欢", "喜歡"));

                put(2, Arrays.asList("2", "swt2", "wah", "panic",
                        "헉", "어", "어머", "깜짝", "놀래", "당황",
                        "驚いた", "びっくり", "わー", "哇", "惊讶", "驚訝"));

                put(3, Arrays.asList("3", "?", "뭐", "뭐야", "왜", "어", "음",
                        "何", "なに", "なぜ", "什么", "为什么", "什麼", "為什麼"));

                put(4, Arrays.asList("4", "!", "아", "어", "헉", "와", "우와",
                        "あ", "おお", "うわ", "啊", "哇", "呀"));

                put(5, Arrays.asList("5", "...", "음", "글쎄", "생각", "고민",
                        "うーん", "そうね", "嗯", "想想", "嗯嗯"));

                put(6, Arrays.asList("6", "an", "anger", "angry",
                        "화", "화나", "화남", "짜증", "빡", "빡쳐", "열받", "어이", "진짜", "아오", "아", "억울",
                        "怒り", "むかつく", "いらいら", "腹立つ", "气", "生气", "愤怒", "氣", "生氣", "憤怒"));

                put(7, Arrays.asList("7", "note", "ho", "호", "후", "흠", "음악", "노래",
                        "ほう", "ふーん", "哼", "呵", "音符"));

                put(8, Arrays.asList("8", "sob", "cry", "sad",
                        "슬퍼", "슬프", "울어", "울음", "눈물", "ㅠㅠ", "ㅜㅜ", "ㅠ", "ㅜ", "엉엉", "흑흑",
                        "悲しい", "泣く", "涙", "しょぼん", "伤心", "难过", "哭", "眼泪", "傷心", "難過", "眼淚"));

                put(9, Arrays.asList("9", "spit", "rice", "hungry",
                        "배고", "배고파", "배고픔", "밥", "먹고싶", "음식", "허기", "굶주", "맛있",
                        "お腹空いた", "腹減った", "食べたい", "ご飯", "饿", "肚子饿", "想吃", "餓", "肚子餓"));

                put(10, Arrays.asList("10", "spin", "dizzy",
                        "어지", "현기", "돌아", "빙빙", "헤롱", "멍", "헉",
                        "めまい", "ふらふら", "くるくる", "头晕", "晕", "頭暈"));

                put(11, Arrays.asList("11", "find", "??", "찾", "어디", "모르", "헷갈", "????",
                        "探す", "どこ", "わからない", "찾아", "找", "不知道", "不懂"));

                put(12, Arrays.asList("12", "omg", "shock",
                        "헉", "어머", "세상", "대박", "와", "우와", "놀라", "깜짝", "어쩌지", "진짜",
                        "マジ", "うそ", "びっくり", "すごい", "天哪", "我的天", "哇塞", "真的", "厲害"));

                put(13, Arrays.asList("13", "ok", "nod", "네", "응", "예", "맞", "좋", "오케", "알겠",
                        "はい", "うん", "そう", "いいね", "オッケー", "好", "对", "是", "OK", "好的", "對"));

                put(14, Arrays.asList("14", "fsh", "flash", "+_+", "번쩍", "반짝", "빛", "플래시",
                        "キラキラ", "ピカピカ", "闪", "亮", "閃"));

                put(15, Arrays.asList("15", "kiss", "kis", "뽀뽀", "키스", "쪽", "츄", "💋",
                        "キス", "チュー", "吻", "亲", "親"));

                put(16, Arrays.asList("16", "lol", "ha", "heh",
                        "웃", "웃음", "하하", "호호", "히히", "ㅋㅋ", "ㅎㅎ", "크크", "킥킥", "푸하하",
                        "笑い", "はは", "ひひ", "くく", "ふふ", "ワロタ", "哈哈", "呵呵", "嘻嘻", "笑"));

                put(17, Arrays.asList("17", "gg", "giggle", "키득", "킥킥", "히히", "깔깔",
                        "くすくす", "ひひ", "咯咯", "嘻嘻"));

                put(18, Arrays.asList("18", "sigh", "한숨", "후", "휴", "아이고", "에휴", "하",
                        "ため息", "はあ", "叹气", "唉", "歎氣"));

                put(19, Arrays.asList("19", "meh", "lick", "핥", "맛", "냠냠", "혀", "맛있",
                        "ぺろ", "舔", "舌头", "舌頭"));

                put(20, Arrays.asList("20", "orz", "otl", "좌절", "절망", "포기", "망", "죽겠",
                        "絶望", "だめ", "やばい", "完了", "绝望", "完蛋", "絕望"));

                put(21, Arrays.asList("21", "o", "oh", "yes", "오", "우", "와", "네", "그래",
                        "おお", "うん", "はい", "哦", "噢", "是", "好"));

                put(22, Arrays.asList("22", "x", "no", "아니", "안돼", "싫", "노", "절대",
                        "いや", "だめ", "ダメ", "いいえ", "不", "不要", "不行"));

                put(23, Arrays.asList("23", "!?", "surprised", "어?", "뭐?", "헉?", "진짜?", "설마?",
                        "え？", "マジ？", "うそ？", "什么？", "真的？", "什麼？", "真的？"));

                put(24, Arrays.asList("24", "rock", "bawi", "바위", "주먹", "가위바위보",
                        "グー", "石头", "石頭"));

                put(25, Arrays.asList("25", "paper", "bo", "보", "종이", "펼쳐",
                        "パー", "布", "手掌"));

                put(26, Arrays.asList("26", "scissors", "gawi", "ya", "yeah", "가위", "브이", "예", "야",
                        "チョキ", "ピース", "剪刀", "耶"));

                put(27, Arrays.asList("27", "-w-", "만족", "흐뭇", "뿌듯", "좋아", "기분좋",
                        "満足", "いい感じ", "满意", "滿意"));

                put(28, Arrays.asList("28", "-o-", "놀라", "멍", "어리", "기절",
                        "ぽかん", "呆", "惊呆", "驚呆"));

                put(29, Arrays.asList("29", "blink", "wink", "윙크", "깜빡", "눈감", "애교",
                        "ウィンク", "眨眼", "挤眼", "擠眼"));

                put(30, Arrays.asList("30", "pif", "프", "퍽", "톡", "콕",
                        "ぷ", "噗", "戳"));

                put(31, Arrays.asList("31", "shy", "shine", "부끄", "쑥스", "창피", "민망", "얼굴빨갛",
                        "恥ずかし", "照れ", "害羞", "脸红", "臉紅"));

                put(32, Arrays.asList("32", "hmm", "음", "흠", "글쎄", "생각", "고민중",
                        "うーん", "そうね", "嗯", "想想"));

                put(33, Arrays.asList("33", ":p", "메롱", "벤", "혀", "장난",
                        "べー", "吐舌头", "吐舌頭"));

                put(34, Arrays.asList("34", "lll", "세로", "줄", "막대",
                        "縦線", "竖线", "豎線"));
            }};

            // nameToIdMap 생성
            nameToIdMap = new HashMap<>();
            List<String> allNames = new ArrayList<>();

            for (Map.Entry<Integer, List<String>> entry : idToNames.entrySet()) {
                int id = entry.getKey();
                List<String> names = entry.getValue();

                for (String name : names) {
                    nameToIdMap.put(name, id);
                    allNames.add(name);
                }
            }

            // nameArray 생성
            nameArray = allNames.toArray(new String[0]);

            // 감정 표현의 우선순위 맵 초기화
            emotionPriorityMap = initializeEmotionPriority();
        }

        /**
         * 감정 표현별 우선순위 설정
         */
        private static Map<String, Integer> initializeEmotionPriority() {
            Map<String, Integer> priorityMap = new HashMap<>();

            // 강한 감정 표현 (높은 우선순위)
            String[] highPriority = {"사랑", "love", "♥", "❤", "愛", "好き", "爱", "喜欢",
                    "화나", "빡", "빡쳐", "열받", "angry", "怒り", "むかつく", "气", "생기", "愤怒",
                    "슬퍼", "울어", "ㅠㅠ", "ㅜㅜ", "cry", "sad", "悲しい", "泣く", "伤心", "哭",
                    "ㅋㅋ", "하하", "lol", "웃음", "笑い", "はは", "哈哈", "笑",
                    "놀라", "헉", "와", "omg", "대박", "マジ", "びっくり", "天哪", "哇塞"};

            String[] mediumPriority = {"좋", "네", "응", "ok", "yes", "はい", "うん", "好", "对",
                    "아니", "싫", "no", "いや", "だめ", "不", "불요",
                    "배고", "hungry", "お腹空いた", "饿",
                    "부끄", "shy", "恥ずかし", "害羞"};

            String[] lowPriority = {"?", "!", "...", "음", "흠", "何", "なに", "什么", "嗯",
                    "어", "아", "おお", "あ", "哦", "啊"};

            // 우선순위 할당
            for (String word : highPriority) {
                priorityMap.put(word.toLowerCase(), 10);
            }
            for (String word : mediumPriority) {
                priorityMap.put(word.toLowerCase(), 7);
            }
            for (String word : lowPriority) {
                priorityMap.put(word.toLowerCase(), 3);
            }

            return priorityMap;
        }

        /**
         * 감정 표현의 우선순위 반환
         */
        private static int getEmotionPriority(String word) {
            return emotionPriorityMap.getOrDefault(word.toLowerCase(), 1);
        }

        /**
         * 기존 메소드: 명령어용 (우선순위 무시)
         */
        public int getEmoteId(String name) {
            if (name == null) return 0;
            return nameToIdMap.getOrDefault(name, -1);
        }

        /**
         * 채팅 메시지에서 가장 적절한 이모티콘을 찾습니다 (우선순위 적용)
         */
        public static int findBestEmoteForMessage(String message) {
            if (message == null || message.trim().isEmpty()) return -1;

            String lowerMessage = message.toLowerCase();
            int bestEmoteId = -1;
            int highestPriority = -1;

            for (Map.Entry<String, Integer> entry : nameToIdMap.entrySet()) {
                String name = entry.getKey();
                int emoteId = entry.getValue();

                if (lowerMessage.contains(name.toLowerCase())) {
                    int priority = getEmotionPriority(name);

                    if (priority > highestPriority) {
                        highestPriority = priority;
                        bestEmoteId = emoteId;
                    }
                }
            }

            return bestEmoteId;
        }

        /**
         * 더 정교한 매칭 (단어 경계 고려)
         */
        public int findBestEmoteForMessageStrict(String message) {
            if (message == null || message.trim().isEmpty()) return -1;

            String lowerMessage = message.toLowerCase();
            int bestEmoteId = -1;
            int highestPriority = -1;

            for (Map.Entry<String, Integer> entry : nameToIdMap.entrySet()) {
                String name = entry.getKey();
                int emoteId = entry.getValue();
                int priority = getEmotionPriority(name);

                boolean found = false;

                // 특수 문자나 짧은 감정표현은 포함 검사
                if (name.length() <= 2 || name.matches(".*[^a-zA-Z가-힣].*")) {
                    found = lowerMessage.contains(name.toLowerCase());
                }
                // 긴 단어는 단어 경계 검사
                else {
                    found = lowerMessage.matches(".*\\b" + Pattern.quote(name.toLowerCase()) + "\\b.*");
                }

                if (found && priority > highestPriority) {
                    highestPriority = priority;
                    bestEmoteId = emoteId;
                }
            }

            return bestEmoteId;
        }

        // 기존 메소드들
        public String[] getAllNames() {
            return nameArray;
        }

        public Set<String> getNameSet() {
            return nameToIdMap.keySet();
        }

        public int getEmoteCount() {
            return nameToIdMap.size();
        }
    }

    // 통합된 이모트 데이터 - 기존 EmoNameArray와 EmoNameToID 대체
    public static final EmoteData EMOTE_DATA = new EmoteData();

    // 명령어 별칭 목록
    private static final List<String> ALIASES = new ArrayList<>() {{
        add("em");
        add("emo");
        add("emote");
        add("emotes");
    }};

    public ShipCmdEmotes() {
    }

    /**
     * 이모트 이름을 ID로 변환 (기존 nameToEmoID 메소드 대체)
     * @param emoteName 이모트 이름
     * @return 이모트 ID (유효하지 않으면 0 반환)
     */
    public static int nameToEmoID(String emoteName) {
        return EMOTE_DATA.getEmoteId(emoteName);
    }

    /**
     * 명령어 이름 반환
     */
    @Override
    public String getName() {
        return "shipemotes";
    }

    /**
     * 명령어 별칭 반환
     */
    @Override
    public List<String> getAliases() {
        return ALIASES;
    }

    /**
     * 명령어 사용법 가이드 텍스트 생성
     */
    @Override
    public String getUsage(ICommandSender sender) {
        StringBuilder usage = new StringBuilder("/shipemotes [");

        String[] names = EMOTE_DATA.getAllNames();
        for (int i = 0; i < names.length; i++) {
            usage.append(names[i]);
            if (i < names.length - 1) {
                usage.append(", ");
            }
        }

        usage.append("]");
        return usage.toString();
    }

    /**
     * 명령어 권한 레벨 확인 (모든 플레이어 허용)
     */
    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }

    /**
     * 탭 자동완성 지원
     */
    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos pos) {
        return getListOfStringsMatchingLastWord(args, EMOTE_DATA.getAllNames());
    }

    /**
     * 명령어 실행 (서버 사이드 전용)
     */
    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        World world = sender.getEntityWorld();

        // 이모트 ID 결정
        int emoteId;

        // 명령어 인자가 있으면 해당 이모트 사용
        if (args.length > 0) {
            emoteId = nameToEmoID(args[0]);
            if (emoteId == -1){
                if (sender instanceof EntityPlayer) {
                    ((EntityPlayer) sender).sendStatusMessage(new TextComponentTranslation("chat.shincolle:command.wrongemote", args[0]), false);
                }
                return;
            }
        }
        // 인자가 없으면 랜덤 이모트 사용
        else {
            emoteId = world.rand.nextInt(EMOTE_DATA.getEmoteCount());
        }

        // 위치 및 엔티티 타입 결정
        float posX, posY, posZ, height;
        int entityType;  // 0: none, 1: EntityLivingBase, 2: block

        // 송신자가 생명체인 경우
        if (sender instanceof EntityLivingBase) {
            EntityLivingBase livingEntity = (EntityLivingBase) sender;

            height = livingEntity.height * 0.25F;
            entityType = 1;

            posX = (float) livingEntity.posX;
            posY = (float) livingEntity.posY;
            posZ = (float) livingEntity.posZ;

            // 플레이어인 경우 높이 조정
            if (sender instanceof EntityPlayer) {
                height = livingEntity.height * 0.65F;
            }
        }
        // 송신자가 명령 블록인 경우
        else if (sender instanceof CommandBlockBaseLogic) {
            height = 0.5F;
            entityType = 2;

            posX = (float) sender.getPosition().getX() + 0.5F;
            posY = (float) sender.getPosition().getY();
            posZ = (float) sender.getPosition().getZ() + 0.5F;
        }
        // 기타 송신자
        else {
            height = 0.5F;
            entityType = 0;

            posX = (float) sender.getPositionVector().x;
            posY = (float) sender.getPositionVector().y;
            posZ = (float) sender.getPositionVector().z;
        }

        // 네트워크로 이모트 파티클 전송
        TargetPoint targetPoint = new TargetPoint(world.provider.getDimension(), posX, posY, posZ, 64D);

        if (sender instanceof Entity) {
            CommonProxy.channelP.sendToAllAround(
                    new S2CSpawnParticle((Entity) sender, 36, height, entityType, emoteId),
                    targetPoint
            );
        } else {
            CommonProxy.channelP.sendToAllAround(
                    new S2CSpawnParticle(36, posX, posY, posZ, height, entityType, emoteId),
                    targetPoint
            );
        }
    }
}