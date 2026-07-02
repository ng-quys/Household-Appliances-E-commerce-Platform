# Cấu hình AI phân loại ảnh sản phẩm

Tính năng đã thêm:

- API backend: `POST /admin/products/ai-classify`
- Nhận ảnh upload từ trang thêm/sửa sản phẩm admin.
- Gửi ảnh + danh sách thể loại/hãng hiện có trong database cho AI.
- AI trả về `categoryId`, `categoryName`, `brandId`, `brandName`, `confidence`.
- Frontend tự chọn dropdown Thể loại và Hãng sản xuất.
- Nếu độ tin cậy thấp hơn `0.7` hoặc thiếu thể loại/hãng, trang sẽ hiện cảnh báo để admin kiểm tra lại.

## Cách dùng Gemini

Trong môi trường chạy app, cấu hình:

```bash
AI_PROVIDER=gemini
GEMINI_API_KEY=your_gemini_api_key_here
```

Hoặc dùng chung biến:

```bash
AI_PROVIDER=gemini
AI_API_KEY=your_gemini_api_key_here
```

## Cách dùng OpenAI Vision

Trong môi trường chạy app, cấu hình:

```bash
AI_PROVIDER=openai
OPENAI_API_KEY=your_openai_api_key_here
```

Hoặc dùng chung biến:

```bash
AI_PROVIDER=openai
AI_API_KEY=your_openai_api_key_here
```

## Tuỳ chỉnh model và ngưỡng tin cậy

```bash
AI_OPENAI_MODEL=gpt-4.1-mini
AI_GEMINI_MODEL=gemini-2.5-flash
AI_CONFIDENCE_THRESHOLD=0.7
```

## Lưu ý bảo mật

Không đưa API key vào JavaScript, HTML hoặc frontend. API key chỉ được đặt trong biến môi trường hoặc file cấu hình backend.
