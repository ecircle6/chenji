# -*- coding: utf-8 -*-
"""生成分享卡片效果图对比页 share-card-render.html（设计稿 vs 实际渲染，PNG 内嵌 base64）"""
import base64
import datetime

def b64(p):
    with open(p, "rb") as f:
        return base64.b64encode(f.read()).decode()

def ref_card(kind):
    if kind == "birthday":
        tag, tagcls, title, date = "🎂 生日提醒", "", "小明", "1998年8月14日 · 阳历"
    elif kind == "marriage":
        tag, tagcls, title, date = "💕 纪念日提醒", " marriage", "结婚三周年", "2023年8月14日 · 阳历"
    else:
        tag, tagcls, title, date = "💑 情侣纪念", " love", "在一起三周年", "2023年8月14日 · 阳历"
    return (
        '<div class="card card-a">'
        '<div class="aurora"><div class="g"></div><div class="p"></div></div>'
        '<div class="pad">'
        '<div class="brand-row"><div class="brand-logo">🌙</div><div class="brand-text">CHENJI</div></div>'
        '<div class="glass">'
        '<div class="tag%s">%s</div>'
        '<div class="title">%s</div>'
        '<div class="date">%s</div>'
        '<div class="count-row"><span class="count-num">364</span><span class="count-unit">天后</span></div>'
        '</div>'
        '<div class="tri">'
        '<div class="col c1"><div class="v">8月</div><div class="k">月份</div></div>'
        '<div class="col c2"><div class="v">14日</div><div class="k">日期</div></div>'
        '<div class="col c3"><div class="v">%s</div><div class="k">星期</div></div>'
        '</div></div></div>'
    ) % (tagcls, tag, title, date, weekday_name(2027, 8, 14))

def weekday_name(y, m, d):
    return "周" + "一二三四五六日"[datetime.date(y, m, d).weekday()]

ref_b = (
    '<div class="card card-b">'
    '<div class="candle-glow"></div>'
    '<div class="pad">'
    '<div class="candle">🕯️</div>'
    '<div class="in-memory">IN MEMORY</div>'
    '<div class="mem-line1">爷爷离开我们已经</div>'
    '<div class="flip"><div class="digits"><div class="d">1</div><div class="d">2</div></div><div class="unit">天</div></div>'
    '<div class="divider"></div>'
    '<div class="poem">"有些人离开了<br>但永远活在记忆里"</div>'
    '<div class="mem-date">农历七月十五 · 2026年</div>'
    '<div class="brand-b">辰 记</div>'
    '</div></div>'
)

pairs = [
    ("A · 极光毛玻璃 — 生日（364 天后）", ref_card("birthday"), "render_A_生日.png"),
    ("A · 极光毛玻璃 — 结婚纪念（364 天后）", ref_card("marriage"), "render_A_纪念日.png"),
    ("A · 极光毛玻璃 — 情侣纪念（364 天后）", ref_card("love"), "render_A_情侣纪念.png"),
    ("B · 深夜烛火 — 缅怀（12 天）", ref_b, "render_B_缅怀.png"),
]

imgs = {n: b64("app/renders/" + n) for _, _, n in pairs}

cards = ""
for title, ref, img in pairs:
    cards += (
        '<section class="pair"><h2>%s</h2><div class="row">'
        '<figure><div class="lab">设计稿 · design-card-demo.html</div>%s</figure>'
        '<figure><div class="lab">实际渲染 · ShareCardGenerator</div>'
        '<img class="card-out" src="data:image/png;base64,%s" alt="%s"></figure>'
        '</div></section>'
    ) % (title, ref, imgs[img], img)

CSS = """
  * { margin:0; padding:0; box-sizing:border-box; }
  body { font-family:"PingFang SC","Microsoft YaHei",sans-serif; background:#14161c; padding:32px 24px 60px; }
  h1 { font-size:22px; text-align:center; color:#e8edf5; margin-bottom:6px; }
  .sub { text-align:center; color:#7d8aa0; font-size:13px; margin-bottom:30px; }
  .pair { max-width:1100px; margin:0 auto 44px; background:#181b23; border:1px solid #232836; border-radius:16px; padding:22px 24px 28px; }
  .pair h2 { font-size:16px; color:#cfe0d8; margin-bottom:18px; font-weight:600; }
  .row { display:flex; gap:26px; justify-content:center; flex-wrap:wrap; align-items:flex-start; }
  figure { display:flex; flex-direction:column; align-items:center; }
  .lab { font-size:12px; color:#8ea0b8; letter-spacing:1px; margin-bottom:10px; }
  .card { width:300px; height:533px; border-radius:24px; position:relative; overflow:hidden; box-shadow:0 20px 60px rgba(0,0,0,.5); }
  img.card-out { width:300px; border-radius:24px; box-shadow:0 20px 60px rgba(0,0,0,.5); display:block; }
  /* 参考稿样式（与 design-card-demo.html 一致） */
  .card-a { background:linear-gradient(135deg,#0c1020 0%,#1a0f2e 50%,#0d1f1f 100%); border:1px solid rgba(255,255,255,.06); }
  .aurora { position:absolute; inset:0; pointer-events:none; }
  .aurora .g { position:absolute; top:-60px; right:-40px; width:200px; height:200px; background:radial-gradient(circle,rgba(0,255,200,.15) 0%,transparent 70%); filter:blur(40px); }
  .aurora .p { position:absolute; bottom:-40px; left:-30px; width:180px; height:180px; background:radial-gradient(circle,rgba(150,100,255,.12) 0%,transparent 70%); filter:blur(40px); }
  .card-a .pad { position:absolute; inset:0; padding:34px 26px; display:flex; flex-direction:column; }
  .brand-row { display:flex; justify-content:space-between; align-items:center; margin-bottom:28px; }
  .brand-logo { width:36px; height:36px; border-radius:12px; background:linear-gradient(135deg,rgba(255,255,255,.10),rgba(255,255,255,.05)); border:1px solid rgba(255,255,255,.08); display:flex; align-items:center; justify-content:center; font-size:18px; }
  .brand-text { color:#8899aa; font-size:12px; letter-spacing:2px; }
  .glass { background:rgba(255,255,255,.04); backdrop-filter:blur(20px); -webkit-backdrop-filter:blur(20px); border:1px solid rgba(255,255,255,.08); border-radius:20px; padding:28px 24px; flex:1; display:flex; flex-direction:column; }
  .tag { color:#66ddaa; font-size:13px; font-weight:500; margin-bottom:12px; }
  .tag.marriage { color:#ff88aa; }
  .tag.love { color:#ff88cc; }
  .title { color:#fff; font-size:26px; font-weight:700; margin-bottom:8px; }
  .date { color:#8899aa; font-size:13px; margin-bottom:20px; }
  .count-row { display:flex; align-items:baseline; margin-top:auto; margin-bottom:auto; }
  .count-num { color:#fff; font-size:48px; font-weight:800; line-height:1; }
  .count-unit { color:#8899aa; font-size:14px; margin-left:8px; }
  .tri { display:flex; gap:10px; margin-top:20px; }
  .tri .col { flex:1; background:rgba(255,255,255,.03); border:1px solid rgba(255,255,255,.06); border-radius:12px; padding:12px; text-align:center; }
  .tri .v { font-size:18px; font-weight:700; }
  .tri .k { font-size:11px; color:#667788; margin-top:3px; }
  .tri .c1 .v { color:#aa88ff; } .tri .c2 .v { color:#66ccff; } .tri .c3 .v { color:#66ddaa; }
  .card-b { background:#0f0f12; border:1px solid rgba(255,255,255,.05); }
  .candle-glow { position:absolute; top:0; left:50%; transform:translateX(-50%); width:120px; height:120px; background:radial-gradient(ellipse at center top,rgba(255,160,60,.12) 0%,transparent 70%); }
  .card-b .pad { position:absolute; inset:0; padding:48px 30px; display:flex; flex-direction:column; align-items:center; text-align:center; }
  .candle { font-size:32px; margin-bottom:12px; }
  .in-memory { color:#cc9977; font-size:11px; letter-spacing:4px; margin-bottom:44px; }
  .mem-line1 { color:#888888; font-size:13px; margin-bottom:12px; }
  .flip { display:flex; align-items:flex-end; justify-content:center; margin-bottom:44px; }
  .flip .digits { display:flex; }
  .flip .d { width:44px; height:52px; margin:0 1px; border-radius:10px; background:linear-gradient(180deg,#2a2a2a,#1a1a1a); border:1px solid rgba(255,255,255,.08); display:flex; align-items:center; justify-content:center; color:#e8c080; font-size:28px; font-weight:800; }
  .flip .unit { color:#666666; font-size:18px; font-weight:500; margin-left:6px; margin-bottom:10px; }
  .divider { width:60%; border-top:1px solid rgba(255,255,255,.06); padding-top:20px; }
  .poem { color:#aaaaaa; font-size:14px; font-style:italic; line-height:1.8; margin-top:20px; }
  .mem-date { color:#666666; font-size:11px; margin-top:16px; }
  .brand-b { color:#555555; font-size:10px; letter-spacing:3px; margin-top:auto; padding-top:24px; }
"""

html = (
    '<!DOCTYPE html>\n<html lang="zh-CN">\n<head>\n<meta charset="UTF-8">\n'
    '<title>辰记 · 分享卡片效果图（实际渲染 vs 设计稿）</title>\n<style>' + CSS + '</style>\n</head>\n<body>\n'
    '<h1>辰记 · 分享卡片效果图</h1>\n'
    '<div class="sub">左＝设计稿定稿（design-card-demo.html 同款样式）｜右＝ShareCardGenerator 实际渲染 PNG（1080×1920，真实日期计算倒计时）</div>\n'
    + cards +
    '\n</body>\n</html>\n'
)

with open("share-card-render.html", "w", encoding="utf-8") as f:
    f.write(html)
print("已生成 share-card-render.html，大小", len(html) // 1024, "KB")
