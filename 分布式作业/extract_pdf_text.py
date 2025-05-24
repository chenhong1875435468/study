import PyPDF2
import re

# 打开PDF文件
pdf_path = 'Achilles_ Efficient TEE-Assisted BFT Consensus via Rollback Resilient Recovery.pdf'
with open(pdf_path, 'rb') as file:
    pdf_reader = PyPDF2.PdfReader(file)
    text = ''
    # 遍历每一页提取文本
    for page in pdf_reader.pages:
        page_text = page.extract_text()
        # 替换换行符为空格（保持段落连贯）
        page_text_clean = page_text.replace('\n', ' ')
        # 过滤XML非法字符（保留XML允许的字符范围）
        page_text_clean = re.sub(r'[^\x09\x0A\x0D\x20-\uD7FF\uE000-\uFFFD\U00100000-\U0010FFFF]', '', page_text_clean)
        text += page_text_clean

# 识别并保留引用角标（如[1][2]形式）
# 使用正则表达式确保角标与正文正确关联
processed_text = re.sub(r'(?<!\[)(\d+)\]', '[\1]', text)  # 修正可能的角标格式问题

# 保存整个PDF的文本到all_text.txt
with open('all_text.txt', 'w', encoding='utf-8') as out_file:
    out_file.write(text)

print('PDF Introduction部分提取完成，已保存至introduction_original.txt')