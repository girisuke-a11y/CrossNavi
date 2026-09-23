#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
CrossNavi - 一般信用（SBI証券・楽天証券）在庫データ自動集約＆配信スクリプト (Option A)

【概要】
証券会社への直接ログインを行わず、公開されている速報データおよび銘柄マスターに基づき
全1,350銘柄の一般信用在庫状況（残株数・ステータス）をJSONとして集約・出力します。
GitHub Actions等のクラウド環境で定期実行され、スマホアプリへの自動配信を実現します。
"""

import json
import os
import sys
from datetime import datetime
from http.server import HTTPServer, SimpleHTTPRequestHandler

OUTPUT_FILE = "inventory_latest.json"

# 超人気・争奪戦銘柄（SBI/楽天ともに在庫なし・0株）
OUT_OF_STOCK_CODES = {
    "7421": "カッパ・クリエイト",
    "7412": "アトム",
    "7616": "コロワイド",
    "7550": "ゼンショーHD",
    "3397": "トリドールHD",
    "7581": "サイゼリヤ",
    "3048": "ビックカメラ",
    "2702": "日本マクドナルドHD",
    "3197": "すかいらーくHD",
    "9861": "吉野家HD",
    "3196": "ホットランド",
    "7674": "NATTY SWANKY (ダンダダン)",
    "2695": "くら寿司"
}

# 大型・在庫潤沢銘柄（多量残数）
LARGE_CAP_STOCKS = {
    "9202": {"sbi": 145000, "rakuten": 8400, "name": "ANAホールディングス"},
    "9201": {"sbi": 98000, "rakuten": 45000, "name": "日本航空 (JAL)"},
    "8267": {"sbi": 82000, "rakuten": 0, "name": "イオン"},
    "9831": {"sbi": 6000, "rakuten": 0, "name": "ヤマダHD"},
    "8591": {"sbi": 350000, "rakuten": 120000, "name": "オリックス"},
    "9434": {"sbi": 0, "rakuten": 0, "name": "ソフトバンク"},
    "9432": {"sbi": 450000, "rakuten": 300000, "name": "NTT"},
    "7203": {"sbi": 280000, "rakuten": 190000, "name": "トヨタ自動車"}
}

# ---------------------------------------------------------
# 今後の拡張用：外部の優待クロスサイト等からのスクレイピング関数
# ---------------------------------------------------------
import re
try:
    from playwright.sync_api import sync_playwright
except ImportError:
    sync_playwright = None

def parse_quantity(text):
    if not text: return 0
    t = text.strip()
    if t in ["◎", "○"]: return 10000
    if t == "△": return 1000
    if t in ["×", "-", "無"]: return 0
    nums = re.findall(r'\d+', t.replace(',', ''))
    if nums:
        return int(nums[0])
    return 0

def fetch_real_inventory_data():
    """
    Playwrightを用いた実際の優待クロスまとめサイトからの在庫データ取得
    """
    if not sync_playwright:
        print("[Scraping Warn] Playwrightがインストールされていません。")
        return None
        
    scraped_data = {}
    try:
        with sync_playwright() as p:
            browser = p.chromium.launch(headless=True)
            context = browser.new_context(
                user_agent="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            )
            page = context.new_page()
            
            # 例: 96ut.com等のクロス一覧ページ
            url = "https://96ut.com/stock/cross.php"
            page.goto(url, timeout=30000, wait_until="domcontentloaded")
            
            # 一般的な表構造（tr / td）を解析
            rows = page.query_selector_all("table tr")
            for row in rows:
                cols = row.query_selector_all("td, th")
                if len(cols) < 5:
                    continue
                
                # 想定: [0] 銘柄コード, [1] 銘柄名, [2] SBI, [3] 楽天 ...
                code_text = cols[0].inner_text().strip()
                if not re.match(r'^\d{4}$', code_text):
                    continue
                    
                name_text = cols[1].inner_text().strip()
                sbi_text = cols[2].inner_text().strip()
                rakuten_text = cols[3].inner_text().strip()
                
                sbi_qty = parse_quantity(sbi_text)
                rakuten_qty = parse_quantity(rakuten_text)
                
                scraped_data[code_text] = {
                    "sbi": sbi_qty,
                    "rakuten": rakuten_qty,
                    "name": name_text
                }
                
            browser.close()
            
        if not scraped_data:
            print("[Scraping Warn] 96ut: データが0件でした。")
            return None
            
        print(f"[Scraping Info] 96ut: {len(scraped_data)} 銘柄の実データを抽出しました。")
        return scraped_data
    except Exception as e:
        print(f"[Scraping Warn] 96ut: 取得に失敗しました: {e}")
        return None

def fetch_real_inventory_data_gokigen():
    """
    第二候補: Gokigen Life 等の別サイトからの在庫データ取得（Playwright使用）
    """
    if not sync_playwright:
        return None
        
    scraped_data = {}
    try:
        with sync_playwright() as p:
            browser = p.chromium.launch(headless=True)
            context = browser.new_context(
                user_agent="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            )
            page = context.new_page()
            
            # 汎用的な別サイトURL例
            url = "https://gokigen-life.tokyo/"
            page.goto(url, timeout=30000, wait_until="domcontentloaded")
            
            # TODO: Gokigen Life等の実際のHTMLテーブルの構造に従って抽出
            # ここではダミーとして処理をスキップし、将来の実装枠組みのみ提供します。
            # （本来は表をクエリして scraped_data に詰めます）
            
            browser.close()
            
        if not scraped_data:
            print("[Scraping Warn] GokigenLife: データが0件でした。")
            return None
            
        print(f"[Scraping Info] GokigenLife: {len(scraped_data)} 銘柄の実データを抽出しました。")
        return scraped_data
    except Exception as e:
        print(f"[Scraping Warn] GokigenLife: 取得に失敗しました: {e}")
        return None

def load_master_stocks():
    """stocks_master.json から銘柄リストをロード"""
    possible_paths = [
        os.path.join("app", "src", "main", "assets", "stocks_master.json"),
        os.path.join("..", "app", "src", "main", "assets", "stocks_master.json"),
        "stocks_master.json"
    ]
    for p in possible_paths:
        if os.path.exists(p):
            try:
                with open(p, "r", encoding="utf-8") as f:
                    return json.load(f)
            except Exception as e:
                print(f"[Warn] {p} load error: {e}")
    return []


def generate_inventory_json(output_path=OUTPUT_FILE):
    """全1,350銘柄の一般信用在庫JSONを生成"""
    now_str = datetime.now().strftime("%Y/%m/%d %H:%M")

    stocks_obj = {}
    source = "none"
    status = "error"
    error_message = ""

    # 第一候補: 96ut.com
    real_data = fetch_real_inventory_data()
    if real_data:
        stocks_obj = real_data
        source = "96ut.com"
        status = "success"
    else:
        # 第二候補: Gokigen Life
        real_data_gokigen = fetch_real_inventory_data_gokigen()
        if real_data_gokigen:
            stocks_obj = real_data_gokigen
            source = "gokigen-life.tokyo"
            status = "success"
        else:
            # 取得失敗時はエラーを記録し、空データを返す（疑似データ生成を廃止）
            status = "error"
            error_message = "All scrapers failed to fetch real inventory data."
            stocks_obj = {}

    payload = {
        "updatedAt": now_str,
        "source": source,
        "status": status,
        "errorMessage": error_message,
        "description": "CrossNavi General Margin Stock Inventory (SBI & Rakuten)",
        "stocks": stocks_obj
    }

    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(payload, f, ensure_ascii=False, indent=2)

    # assets target update if exists
    assets_target = os.path.join("app", "src", "main", "assets", OUTPUT_FILE)
    if os.path.exists(os.path.dirname(assets_target)):
        with open(assets_target, "w", encoding="utf-8") as f:
            json.dump(payload, f, ensure_ascii=False, indent=2)

    print(f"[OK] {output_path} を出力しました (更新時刻: {now_str}, 登録銘柄数: {len(stocks_obj)})")
    return output_path


def start_server(port=8080):
    """PC上でHTTP配信サーバーを起動"""
    import socket
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        s.connect(('8.8.8.8', 80))
        local_ip = s.getsockname()[0]
    except Exception:
        local_ip = '127.0.0.1'
    finally:
        s.close()

    generate_inventory_json()

    print("\n" + "=" * 60)
    print("  CrossNavi 在庫配信サーバーが起動しました")
    print("=" * 60)
    print(f"  PCローカルIP: http://{local_ip}:{port}/{OUTPUT_FILE}")
    print(f"  エミュレータ用: http://10.0.2.2:{port}/{OUTPUT_FILE}")
    print("=" * 60)
    print("  スマホアプリの「↻ 在庫を更新」を押すと同期できます。")
    print("  終了するには Ctrl+C を押してください。\n")

    server_address = ('', port)
    httpd = HTTPServer(server_address, SimpleHTTPRequestHandler)
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        print("\nサーバーを停止しました。")


if __name__ == "__main__":
    if len(sys.argv) > 1 and sys.argv[1] == "--server":
        start_server()
    else:
        generate_inventory_json()
