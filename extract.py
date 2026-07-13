import zipfile
import xml.etree.ElementTree as ET
import sys

def docx_to_text(docx_path):
    try:
        with zipfile.ZipFile(docx_path) as z:
            xml_content = z.read('word/document.xml')
            root = ET.fromstring(xml_content)
            
            # Namespaces
            ns = {'w': 'http://schemas.openxmlformats.org/wordprocessingml/2006/main'}
            
            paragraphs = []
            # Find all paragraph elements w:p
            for p in root.findall('.//w:p', ns):
                p_text = []
                # Find all text elements w:t inside this paragraph
                for t in p.findall('.//w:t', ns):
                    if t.text:
                        p_text.append(t.text)
                paragraphs.append("".join(p_text))
            
            return "\n".join(paragraphs)
    except Exception as e:
        return f"Error: {e}"

if __name__ == "__main__":
    import sys
    if hasattr(sys.stdout, 'reconfigure'):
        sys.stdout.reconfigure(encoding='utf-8')
    if len(sys.argv) < 3:
        print("Usage: extract.py <input_docx> <output_txt>")
        sys.exit(1)
    
    txt = docx_to_text(sys.argv[1])
    with open(sys.argv[2], 'w', encoding='utf-8') as f:
        f.write(txt)
    print(f"Extracted to {sys.argv[2]}")
