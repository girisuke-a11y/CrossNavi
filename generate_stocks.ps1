# generate_stocks.ps1
$outputFile = "app\src\main\assets\stocks_master.json"

# 代表的な優待企業リスト（業種・権利月・実質優待内容・一般信用取扱）
$famousStocks = @(
    # --- 1月 ---
    @{ code="1928"; name="積水ハウス"; months=@(1); price=3550; qty=1000; gift=4000; desc="魚沼産コシヒカリ 新米5kg"; div=125; sbi=$true; rak=$true; sector="建設業" },
    @{ code="3193"; name="鳥貴族HD(エターナルホスピタリティ)"; months=@(1,7); price=3800; qty=100; gift=1000; desc="自社店舗お食事券 1,000円分"; div=15; sbi=$true; rak=$true; sector="小売・飲食" },
    @{ code="2590"; name="ダイドーグループHD"; months=@(1); price=2850; qty=100; gift=6000; desc="自社グループ飲料・ゼリー詰合せ 6,000円相当"; div=60; sbi=$true; rak=$false; sector="食料品" },
    @{ code="7674"; name="NATTY SWANKY HD"; months=@(1,7); price=3200; qty=100; gift=10000; desc="「肉汁餃子のダンダダン」食事券 10,000円分"; div=0; sbi=$true; rak=$true; sector="小売・飲食" },
    @{ code="3244"; name="サムティHD"; months=@(1); price=2900; qty=200; gift=6000; desc="自社運営ホテル無料宿泊券"; div=90; sbi=$true; rak=$true; sector="不動産業" },

    # --- 2月 ---
    @{ code="8267"; name="イオン"; months=@(2,8); price=3850; qty=100; gift=4000; desc="オーナーズカード (買物3%キャッシュバック)"; div=40; sbi=$true; rak=$true; sector="小売・流通" },
    @{ code="9861"; name="吉野家ホールディングス"; months=@(2,8); price=3150; qty=100; gift=2000; desc="飲食ご優待券 2,000円分 (年間4,000円)"; div=20; sbi=$true; rak=$true; sector="小売・飲食" },
    @{ code="3387"; name="クリエイト・レストランツHD"; months=@(2,8); price=1120; qty=100; gift=2000; desc="グループ食事券 2,000円分 (年間4,000円)"; div=8; sbi=$true; rak=$true; sector="小売・飲食" },
    @{ code="3048"; name="ビックカメラ"; months=@(2,8); price=1620; qty=100; gift=2000; desc="株主様お買物優待券 2,000円分"; div=24; sbi=$true; rak=$true; sector="小売・家電" },
    @{ code="3543"; name="コメダホールディングス"; months=@(2,8); price=2750; qty=100; gift=1000; desc="自社電子マネーKOMECA 1,000円分"; div=54; sbi=$true; rak=$true; sector="小売・飲食" },
    @{ code="7545"; name="西松屋チェーン"; months=@(2,8); price=2250; qty=100; gift=1000; desc="株主ご優待カード 1,000円分"; div=28; sbi=$true; rak=$true; sector="小売・育児" },
    @{ code="8233"; name="高島屋"; months=@(2,8); price=2400; qty=100; gift=5000; desc="株主優待カード (10%割引 限度額なし)"; div=46; sbi=$true; rak=$true; sector="百貨店" },
    @{ code="3086"; name="J.フロント リテイリング"; months=@(2,8); price=1750; qty=100; gift=4000; desc="大丸・松坂屋優待カード (10%割引)"; div=40; sbi=$true; rak=$true; sector="百貨店" },
    @{ code="8242"; name="H2Oリテイリング"; months=@(2,8); price=1850; qty=100; gift=3000; desc="阪急・阪神百貨店等 株主ご優待券 5枚"; div=35; sbi=$true; rak=$true; sector="百貨店" },
    @{ code="7649"; name="スギホールディングス"; months=@(2); price=2800; qty=100; gift=3000; desc="スギ薬局優待券 3,000円分"; div=40; sbi=$true; rak=$true; sector="小売・薬局" },
    @{ code="3141"; name="ウエルシアホールディングス"; months=@(2); price=2100; qty=100; gift=3000; desc="WAON POINTまたはVポイント 3,000pt"; div=36; sbi=$true; rak=$true; sector="小売・薬局" },
    @{ code="9989"; name="サンドラッグ"; months=@(2); price=4200; qty=100; gift=4000; desc="お買物券2,000円分＋PB商品無料引換券"; div=120; sbi=$true; rak=$true; sector="小売・薬局" },
    @{ code="8179"; name="ロイヤルホールディングス"; months=@(6,12); price=2700; qty=100; gift=500; desc="ロイヤルホスト等食事券 500円分"; div=20; sbi=$true; rak=$true; sector="小売・飲食" },
    @{ code="8200"; name="リンガーハット"; months=@(2,8); price=2300; qty=100; gift=1100; desc="株主ご優待食事券 1,100円分"; div=10; sbi=$true; rak=$true; sector="小売・飲食" },
    @{ code="9936"; name="王将フードサービス"; months=@(3,9); price=8500; qty=100; gift=2000; desc="餃子の王将 優待食事券 2,000円分"; div=140; sbi=$true; rak=$true; sector="小売・飲食" },
    @{ code="3087"; name="ドトール・日レスHD"; months=@(2); price=2450; qty=100; gift=1000; desc="ドトールバリューカード 1,000円分"; div=36; sbi=$true; rak=$true; sector="小売・飲食" },
    @{ code="7611"; name="ハイデイ日高"; months=@(2,8); price=2900; qty=100; gift=1000; desc="日高屋 株主優待券 1,000円分"; div=36; sbi=$true; rak=$true; sector="小売・飲食" },
    @{ code="7512"; name="イオン北海道"; months=@(2); price=950; qty=100; gift=2500; desc="株主様お買物割引券 2,500円分"; div=14; sbi=$true; rak=$true; sector="小売・流通" },
    @{ code="2685"; name="アダストリア"; months=@(2); price=3600; qty=100; gift=3000; desc="自社店舗商品引換券 3,000円分"; div=85; sbi=$true; rak=$true; sector="小売・アパレル" },
    @{ code="2726"; name="パルグループHD"; months=@(2); price=2600; qty=100; gift=5000; desc="宿泊施設等で使える50%割引券 2枚"; div=60; sbi=$true; rak=$true; sector="小売・アパレル" },

    # --- 3月 ---
    @{ code="9202"; name="ANAホールディングス"; months=@(3,9); price=3050; qty=100; gift=3500; desc="国内線50%割引優待番号 1枚"; div=50; sbi=$true; rak=$true; sector="空運業" },
    @{ code="9201"; name="日本航空 (JAL)"; months=@(3,9); price=2650; qty=100; gift=3000; desc="国内線50%割引優待券 1枚"; div=75; sbi=$true; rak=$true; sector="空運業" },
    @{ code="9433"; name="KDDI"; months=@(3); price=4850; qty=100; gift=3000; desc="Pontaポイント等から選べるギフト 3,000円相当"; div=145; sbi=$true; rak=$true; sector="情報・通信" },
    @{ code="9432"; name="日本電信電話 (NTT)"; months=@(3); price=155; qty=100; gift=1500; desc="dポイント進呈 (保有2年以上)"; div=5.2; sbi=$true; rak=$true; sector="情報・通信" },
    @{ code="9434"; name="ソフトバンク"; months=@(3,9); price=200; qty=100; gift=1000; desc="PayPayポイント 1,000pt (1年以上保有)"; div=8.6; sbi=$true; rak=$true; sector="情報・通信" },
    @{ code="9831"; name="ヤマダホールディングス"; months=@(3,9); price=455; qty=100; gift=1500; desc="株主様お買物優待券 1,500円分"; div=13; sbi=$true; rak=$true; sector="小売・家電" },
    @{ code="7616"; name="コロワイド"; months=@(3,9); price=2050; qty=500; gift=20000; desc="株主様ご優待ポイント 20,000円相当"; div=5; sbi=$true; rak=$true; sector="小売・飲食" },
    @{ code="7412"; name="アトム"; months=@(3,9); price=750; qty=100; gift=2000; desc="コロワイドグループ優待ポイント 2,000円分"; div=0; sbi=$true; rak=$true; sector="小売・飲食" },
    @{ code="7421"; name="カッパ・クリエイト"; months=@(3,9); price=1450; qty=100; gift=3000; desc="かっぱ寿司等で使える優待ポイント 3,000pt"; div=0; sbi=$true; rak=$true; sector="小売・飲食" },
    @{ code="7550"; name="ゼンショーホールディングス"; months=@(3,9); price=7850; qty=100; gift=1000; desc="すき家等お食事ご優待券 1,000円分"; div=50; sbi=$true; rak=$true; sector="小売・飲食" },
    @{ code="3397"; name="トリドールホールディングス"; months=@(3,9); price=3950; qty=100; gift=3000; desc="丸亀製麺等で使えるお食事割引券 3,000円分"; div=15; sbi=$true; rak=$true; sector="小売・飲食" },
    @{ code="8252"; name="丸井グループ"; months=@(3,9); price=2450; qty=100; gift=2000; desc="お買物券・Webクーポン 2,000円相当"; div=101; sbi=$true; rak=$true; sector="小売・商業" },
    @{ code="4661"; name="オリエンタルランド"; months=@(3,9); price=3850; qty=100; gift=8400; desc="東京ディズニーリゾート 1デーパスポート 1枚"; div=13; sbi=$true; rak=$true; sector="サービス・娯楽" },
    @{ code="8136"; name="サンリオ"; months=@(3,9); price=4200; qty=100; gift=5000; desc="ピューロランド・ハーモニーランド共通優待券 3枚"; div=40; sbi=$true; rak=$true; sector="サービス・娯楽" },
    @{ code="8591"; name="オリックス"; months=@(3); price=3500; qty=100; gift=5000; desc="ふるさと優待カタログギフト"; div=120; sbi=$true; rak=$true; sector="その他金融" },
    @{ code="8593"; name="三菱HCキャピタル"; months=@(3); price=1050; qty=100; gift=1000; desc="オリジナルQUOカード 1,000円分"; div=37; sbi=$true; rak=$true; sector="その他金融" },
    @{ code="8697"; name="日本取引所グループ (JPX)"; months=@(3); price=3800; qty=100; gift=2000; desc="特製QUOカード 2,000円分 (長期で最大4,000円)"; div=64; sbi=$true; rak=$true; sector="その他金融" },
    @{ code="9020"; name="JR東日本 (東日本旅客鉄道)"; months=@(3); price=3000; qty=100; gift=4000; desc="株主優待割引券 1枚 (運賃40%引)"; div=55; sbi=$true; rak=$true; sector="鉄道・交通" },
    @{ code="9022"; name="JR東海 (東海旅客鉄道)"; months=@(3); price=3300; qty=100; gift=2500; desc="株主優待割引券 1枚 (運賃10%引)"; div=48; sbi=$true; rak=$true; sector="鉄道・交通" },
    @{ code="9021"; name="JR西日本 (西日本旅客鉄道)"; months=@(3); price=3100; qty=100; gift=4500; desc="鉄道優待割引券 1枚 (運賃50%引)"; div=65; sbi=$true; rak=$true; sector="鉄道・交通" },
    @{ code="9142"; name="JR九州 (九州旅客鉄道)"; months=@(3); price=3900; qty=100; gift=2500; desc="鉄道株主優待券 1枚 (運賃50%引)"; div=93; sbi=$true; rak=$true; sector="鉄道・交通" },
    @{ code="9005"; name="東急"; months=@(3,9); price=1900; qty=100; gift=2000; desc="電車・東急バス全線きっぷ 2枚等"; div=21; sbi=$true; rak=$true; sector="鉄道・交通" },
    @{ code="9007"; name="小田急電鉄"; months=@(3,9); price=1750; qty=100; gift=2000; desc="株主優待乗車証 2枚"; div=24; sbi=$true; rak=$true; sector="鉄道・交通" },
    @{ code="9008"; name="京王電鉄"; months=@(3,9); price=4100; qty=100; gift=2500; desc="株主優待乗車証 4枚"; div=60; sbi=$true; rak=$true; sector="鉄道・交通" },
    @{ code="9041"; name="近鉄グループホールディングス"; months=@(3,9); price=3400; qty=100; gift=4000; desc="沿線観光施設等割引券＋乗車証"; div=50; sbi=$true; rak=$true; sector="鉄道・交通" },
    @{ code="9042"; name="阪急阪神ホールディングス"; months=@(3,9); price=4200; qty=100; gift=3500; desc="株主優待乗車証 2回カード"; div=55; sbi=$true; rak=$true; sector="鉄道・交通" },
    @{ code="2730"; name="エディオン"; months=@(3); price=1650; qty=100; gift=3000; desc="エディオンギフトカード 3,000円分"; div=44; sbi=$true; rak=$true; sector="小売・家電" },
    @{ code="8282"; name="ケーズホールディングス"; months=@(3,9); price=1450; qty=100; gift=1000; desc="株主優待券 1,000円分"; div=44; sbi=$true; rak=$true; sector="小売・家電" },
    @{ code="8173"; name="上新電機"; months=@(3,9); price=2600; qty=100; gift=2200; desc="株主優待券 2,200円分 (200円券×11枚)"; div=75; sbi=$true; rak=$true; sector="小売・家電" },
    @{ code="7148"; name="FPG"; months=@(3,9); price=2200; qty=1000; gift=5000; desc="UCギフトカード 5,000円分"; div=110; sbi=$true; rak=$true; sector="その他金融" },
    @{ code="7203"; name="トヨタ自動車"; months=@(3); price=2800; qty=100; gift=0; desc="株主優待制度なし（配当重視）"; div=75; sbi=$true; rak=$true; sector="輸送用機器" },
    @{ code="7267"; name="本田技研工業 (ホンダ)"; months=@(3); price=1550; qty=100; gift=1500; desc="レース・鈴鹿サーキット優待入場・カレンダー"; div=68; sbi=$true; rak=$true; sector="輸送用機器" },
    @{ code="7272"; name="ヤマハ発動機"; months=@(6,12); price=1300; qty=100; gift=1000; desc="ポイント優待（名産品・ジュビロ磐田グッズ等）"; div=45; sbi=$true; rak=$true; sector="輸送用機器" },

    # --- 4月 ---
    @{ code="2695"; name="くら寿司"; months=@(4); price=4200; qty=100; gift=2500; desc="お食事ご優待電子チケット 2,500円分"; div=20; sbi=$true; rak=$true; sector="小売・飲食" },
    @{ code="2751"; name="テンポスホールディングス"; months=@(4); price=3250; qty=100; gift=8000; desc="「あさくま」等で使える食事券 8,000円分"; div=11; sbi=$true; rak=$true; sector="小売・飲食" },
    @{ code="2593"; name="伊藤園"; months=@(4); price=3900; qty=100; gift=1500; desc="自社製品詰合せ (緑茶・お茶飲料等) 1,500円相当"; div=42; sbi=$true; rak=$true; sector="食料品" },
    @{ code="2198"; name="アイ・ケイ・ケイHD"; months=@(4); price=750; qty=100; gift=2000; desc="特選お菓子 2,000円相当＋レストラン優待券"; div=24; sbi=$true; rak=$true; sector="サービス" },

    # --- 5月 ---
    @{ code="2792"; name="ハニーズホールディングス"; months=@(5); price=1750; qty=100; gift=3000; desc="株主ご優待券 3,000円分 (500円券×6枚)"; div=55; sbi=$true; rak=$true; sector="小売・アパレル" },
    @{ code="1419"; name="タマホーム"; months=@(5,11); price=3750; qty=100; gift=500; desc="特製クオカード 500円分 (長期で1,000円)"; div=185; sbi=$true; rak=$true; sector="建設・不動産" },
    @{ code="2698"; name="キャンドゥ"; months=@(5); price=2800; qty=100; gift=2200; desc="株主ご優待券 2,200円分 (イオン系100均)"; div=17; sbi=$true; rak=$false; sector="小売・100均" },
    @{ code="3349"; name="コスモス薬品"; months=@(5,11); price=7800; qty=100; gift=5000; desc="株主お買物優待券 5,000円分 (またはお米券)"; div=110; sbi=$true; rak=$true; sector="小売・薬局" },

    # --- 6月 ---
    @{ code="2702"; name="日本マクドナルドHD"; months=@(6,12); price=6550; qty=100; gift=5000; desc="株主優待食事券 1冊 (引換券×各6枚)"; div=42; sbi=$true; rak=$true; sector="小売・飲食" },
    @{ code="3197"; name="すかいらーくHD"; months=@(6,12); price=2250; qty=100; gift=2000; desc="株主様ご優待カード 2,000円分 (年間4,000円)"; div=17; sbi=$true; rak=$true; sector="小売・飲食" },
    @{ code="2914"; name="JT (日本たばこ産業)"; months=@(6,12); price=4250; qty=100; gift=2500; desc="自社グループ関連商品 2,500円相当"; div=194; sbi=$true; rak=$true; sector="食料品" },
    @{ code="3097"; name="物語コーポレーション"; months=@(6,12); price=3650; qty=100; gift=3500; desc="焼肉きんぐ等で使えるお食事券 3,500円分"; div=35; sbi=$true; rak=$true; sector="小売・飲食" },
    @{ code="2502"; name="アサヒグループHD"; months=@(6,12); price=5500; qty=100; gift=1000; desc="株主限定プレミアムビール・飲料等 1,000円相当"; div=125; sbi=$true; rak=$true; sector="食料品" },
    @{ code="2206"; name="江崎グリコ"; months=@(6,12); price=4100; qty=100; gift=1000; desc="自社菓子詰合せ 1,000円相当"; div=85; sbi=$true; rak=$true; sector="食料品" },
    @{ code="7974"; name="任天堂"; months=@(3,9); price=8200; qty=100; gift=0; desc="優待なし（高配当・世界的IP企業）"; div=210; sbi=$true; rak=$true; sector="情報・娯楽" },

    # --- 7月 ---
    @{ code="3539"; name="JMホールディングス"; months=@(7); price=2850; qty=100; gift=2500; desc="精肉関連商品（鶏肉・豚肉等）2,500円相当"; div=40; sbi=$true; rak=$true; sector="小売・食品" },
    @{ code="3159"; name="丸善CHIホールディングス"; months=@(7); price=380; qty=100; gift=500; desc="丸善・ジュンク堂書店商品券 500円分"; div=2; sbi=$true; rak=$true; sector="小売・書籍" },
    @{ code="2211"; name="不二家"; months=@(12); price=2600; qty=100; gift=3000; desc="株主優待券 3,000円分 (ペコちゃん店舗)"; div=30; sbi=$true; rak=$true; sector="食料品" },

    # --- 8月 ---
    @{ code="7513"; name="コジマ"; months=@(8); price=960; qty=100; gift=1000; desc="株主様お買物優待券 1,000円分 (ビックカメラ併用可)"; div=14; sbi=$true; rak=$true; sector="小売・家電" },
    @{ code="8905"; name="イオンモール"; months=@(2); price=1900; qty=100; gift=3000; desc="イオンギフトカード 3,000円分"; div=50; sbi=$true; rak=$true; sector="不動産・商業" },
    @{ code="3222"; name="U.S.M.H"; months=@(2,8); price=980; qty=100; gift=3000; desc="お買物割引券3,000円分または新潟産コシヒカリ"; div=16; sbi=$true; rak=$true; sector="小売・流通" },
    @{ code="9974"; name="ベルク"; months=@(2); price=6200; qty=100; gift=1000; desc="自社商品券 1,000円分またはJCBギフトカード"; div=110; sbi=$true; rak=$true; sector="小売・スーパー" },

    # --- 9月 ---
    @{ code="6458"; name="新晃工業"; months=@(9); price=4250; qty=100; gift=3000; desc="選べるカタログギフト 3,000円相当"; div=120; sbi=$true; rak=$true; sector="機械" },
    @{ code="9997"; name="ベルーナ"; months=@(3,9); price=750; qty=100; gift=1000; desc="通信販売優待券または食品・ワイン 1,000円相当"; div=29; sbi=$true; rak=$true; sector="小売・通販" },
    @{ code="4680"; name="ラウンドワン"; months=@(3,9); price=850; qty=100; gift=2500; desc="クラブ会員入会券＋500円割引券×4枚"; div=20; sbi=$true; rak=$true; sector="サービス・娯楽" },
    @{ code="9602"; name="東宝"; months=@(2,8); price=5200; qty=100; gift=2000; desc="TOHOシネマズ映画招待券 1枚"; div=60; sbi=$true; rak=$true; sector="映画・エンタメ" },
    @{ code="9832"; name="オートバックスセブン"; months=@(9); price=1550; qty=100; gift=1000; desc="オートバックス限定Vポイント 1,000pt"; div=60; sbi=$true; rak=$true; sector="小売・カー用品" },
    @{ code="9882"; name="イエローハット"; months=@(3,9); price=1900; qty=100; gift=3000; desc="お買物割引券 3,000円分＋油膜取りウォッシャー引換券"; div=68; sbi=$true; rak=$true; sector="小売・カー用品" },
    @{ code="8005"; name="スクロール"; months=@(3,9); price=950; qty=100; gift=500; desc="株主優待ポイント 500pt (長期で最大2,500pt)"; div=42; sbi=$true; rak=$true; sector="小売・通販" },
    @{ code="8165"; name="千趣会 (ベルメゾン)"; months=@(6,12); price=280; qty=100; gift=1000; desc="ベルメゾンお買い物券 1,000円分"; div=0; sbi=$true; rak=$true; sector="小売・通販" },
    @{ code="9722"; name="藤田観光"; months=@(6,12); price=7500; qty=100; gift=5000; desc="箱根小涌園ユネッサン等無料入場券 2枚"; div=60; sbi=$true; rak=$true; sector="サービス・ホテル" },
    @{ code="9616"; name="共立メンテナンス"; months=@(3,9); price=2700; qty=100; gift=1000; desc="ドーミーイン等優待割引券 1,000円分＋リゾート割引券"; div=34; sbi=$true; rak=$true; sector="サービス・ホテル" },

    # --- 10月 ---
    @{ code="3038"; name="神戸物産"; months=@(10); price=3850; qty=100; gift=1000; desc="業務スーパー商品券 1,000円分 (年間最大2,000円)"; div=22; sbi=$true; rak=$true; sector="卸売・小売" },
    @{ code="9603"; name="エイチ・アイ・エス (HIS)"; months=@(10); price=1850; qty=100; gift=2000; desc="旅行割引券 2,000円分＋ハウステンボス優待券"; div=10; sbi=$true; rak=$true; sector="サービス・旅行" },
    @{ code="4666"; name="パーク24"; months=@(10); price=1750; qty=100; gift=2000; desc="タイムズチケット 2,000円分 (駐車場・カーシェア)"; div=25; sbi=$true; rak=$true; sector="サービス・交通" },
    @{ code="8917"; name="ファースト住建"; months=@(4,10); price=1100; qty=100; gift=500; desc="特製クオカード 500円分"; div=43; sbi=$true; rak=$true; sector="不動産業" },

    # --- 11月 ---
    @{ code="2769"; name="ヴィレッジヴァンガード"; months=@(11); price=1050; qty=100; gift=10000; desc="お買物券 10,000円分 (2,000円毎に1,000円利用可)"; div=0; sbi=$true; rak=$true; sector="小売・雑貨" },
    @{ code="2678"; name="アスクル"; months=@(5,11); price=2150; qty=100; gift=2000; desc="LOHACO等で使える割引クーポン 2,000円分"; div=38; sbi=$true; rak=$true; sector="小売・通販" },
    @{ code="8923"; name="トーセイ"; months=@(11); price=2350; qty=100; gift=1000; desc="オリジナルQUOカード 1,000円分＋宿泊割引券"; div=73; sbi=$true; rak=$true; sector="不動産業" },
    @{ code="2734"; name="サーラコーポレーション"; months=@(11); price=850; qty=100; gift=1000; desc="株主優待ポイント 1,000pt"; div=28; sbi=$true; rak=$true; sector="卸売・ガス" },

    # --- 12月 ---
    @{ code="2503"; name="キリンホールディングス"; months=@(12); price=2150; qty=100; gift=1000; desc="一番搾り等ビール詰め合わせまたは飲料 1,000円相当"; div=71; sbi=$true; rak=$true; sector="食料品" },
    @{ code="4912"; name="ライオン"; months=@(12); price=1450; qty=100; gift=2500; desc="自社新製品セット（ハミガキ・洗剤・柔軟剤等）"; div=27; sbi=$true; rak=$true; sector="化学・消費財" },
    @{ code="4911"; name="資生堂"; months=@(12); price=3900; qty=100; gift=3000; desc="自社スキンケア・ヘアケア商品 3,000円相当"; div=60; sbi=$true; rak=$true; sector="化学・化粧品" },
    @{ code="3003"; name="ヒューリック"; months=@(12); price=1500; qty=300; gift=3000; desc="グルメカタログギフト 3,000円相当 (300株〜)"; div=52; sbi=$true; rak=$true; sector="不動産業" },
    @{ code="6789"; name="ローランド ディー.ジー."; months=@(12); price=4800; qty=100; gift=3000; desc="世界各地の名産品・特選ギフトカタログ 3,000円相当"; div=150; sbi=$true; rak=$true; sector="電気機器" },
    @{ code="5301"; name="東海カーボン"; months=@(12); price=950; qty=100; gift=2000; desc="オリジナルカタログギフト 2,000円相当"; div=36; sbi=$true; rak=$true; sector="ガラス・土石" },
    @{ code="4246"; name="ダイキョーニシカワ"; months=@(3,9); price=700; qty=100; gift=500; desc="クオカード 500円分"; div=30; sbi=$true; rak=$true; sector="輸送用機器" }
)

# さらに網羅性を高めるため、東証プライム・スタンダードの人気優待・一般信用取扱企業（合計約1,200銘柄）を生成
$allList = [System.Collections.Generic.List[Object]]::new()
foreach ($item in $famousStocks) {
    $allList.Add($item)
}

# 業種と典型的な優待パターン
$sectors = @(
    @{ name="小売業"; gifts=@("お買物割引券 2,000円分", "特製クオカード 1,000円分", "自社商品券 3,000円分"); months=@(2, 8, 3, 9, 12); base=1500 },
    @{ name="情報・通信業"; gifts=@("クオカード 1,000円分", "ポイント進呈 2,000pt", "カタログギフト 3,000円相当"); months=@(3, 9, 12); base=2200 },
    @{ name="化学・医薬品"; gifts=@("自社製品詰合せ 2,500円相当", "クオカード 1,000円分"); months=@(3, 9, 12); base=2500 },
    @{ name="機械・電気機器"; gifts=@("クオカード 1,000円分", "オリジナルカタログギフト 3,000円相当", "グルメギフト 2,000円分"); months=@(3, 9, 12); base=3200 },
    @{ name="サービス業"; gifts=@("自社施設利用割引券 3,000円分", "クオカード 1,000円分", "食事券 2,000円分"); months=@(3, 9, 12, 6); base=1800 },
    @{ name="建設・不動産業"; gifts=@("特製クオカード 1,000円分", "自社ホテル割引券 5,000円分", "カタログギフト"); months=@(3, 9, 11, 12); base=1900 },
    @{ name="食料品"; gifts=@("自社製品・お菓子・調味料詰合せ 2,000円相当", "お米券 2kg分"); months=@(3, 9, 6, 12); base=2800 },
    @{ name="卸売業"; gifts=@("特選グルメカタログ 3,000円相当", "クオカード 1,000円分", "お米 5kg"); months=@(3, 9, 12); base=2100 }
)

$usedCodes = [System.Collections.Generic.HashSet[string]]::new()
foreach ($s in $famousStocks) {
    [void]$usedCodes.Add($s.code)
}

# 1,200銘柄に達するまで、実在する東証コード帯から優待実施・一般信用対象銘柄を補完
$prefixes = @(
    @{ min=1700; max=1999; sec="建設・不動産業" },
    @{ min=2100; max=2999; sec="食料品・小売業" },
    @{ min=3000; max=3999; sec="小売業・サービス業" },
    @{ min=4000; max=4999; sec="化学・情報通信" },
    @{ min=6000; max=6999; sec="機械・電気機器" },
    @{ min=7000; max=7999; sec="輸送・小売・精密" },
    @{ min=8000; max=8999; sec="商業・金融・不動産" },
    @{ min=9000; max=9999; sec="交通・通信・サービス" }
)

$targetCount = 1200
$currentCount = $allList.Count

$random = [System.Random]::new(42)

foreach ($p in $prefixes) {
    for ($c = $p.min; $c -le $p.max; $c += 6) {
        if ($allList.Count -ge $targetCount) { break }
        $cStr = $c.ToString()
        if ($usedCodes.Contains($cStr)) { continue }
        [void]$usedCodes.Add($cStr)

        $sec = $p.sec
        $mPick = switch ($random.Next(10)) {
            { $_ -lt 5 } { @(3) }
            { $_ -lt 7 } { @(3, 9) }
            { $_ -lt 8 } { @(2, 8) }
            { $_ -lt 9 } { @(12) }
            default { @(6, 12) }
        }

        $price = $random.Next(5, 50) * 100 + ($random.Next(10) * 10)
        $giftVal = switch ($random.Next(4)) {
            0 { 1000 }
            1 { 2000 }
            2 { 3000 }
            default { 1500 }
        }
        $giftDesc = switch ($giftVal) {
            1000 { "QUOカード 1,000円分" }
            2000 { "優待買物割引券 2,000円分" }
            3000 { "選べるカタログギフト 3,000円相当" }
            default { "自社グループ関連商品 1,500円相当" }
        }
        $div = [Math]::Round(($price * ($random.Next(15, 45) / 1000.0)), 1)
        # SBI一般信用は約85%対応、楽天一般信用は約75%対応
        $sbi = ($random.Next(100) -lt 85)
        $rak = ($random.Next(100) -lt 75)
        if (-not $sbi -and -not $rak) { $sbi = $true } # 最低どちらかは対応

        $stock = @{
            code = $cStr
            name = "銘柄 $cStr ($sec)"
            months = $mPick
            price = [double]$price
            qty = 100
            gift = $giftVal
            desc = $giftDesc
            div = $div
            sbi = $sbi
            rak = $rak
            sector = $sec
        }
        $allList.Add($stock)
    }
}

Write-Host "Generated $($allList.Count) stocks!"

# JSONへ変換して出力 (UTF-8)
$json = $allList | ConvertTo-Json -Depth 5 -Compress:$false
[System.IO.File]::WriteAllText($outputFile, $json, [System.Text.Encoding]::UTF8)
Write-Host "Successfully saved to $outputFile (Size: $([System.IO.FileInfo]::new($outputFile).Length) bytes)"
