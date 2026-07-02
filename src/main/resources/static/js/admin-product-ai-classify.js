(function () {
    function getElements() {
        return {
            fileInput: document.getElementById('imageFile'),
            genreSelect: document.getElementById('genreId'),
            brandSelect: document.getElementById('brandId'),
            statusBox: document.getElementById('aiClassifyStatus'),
            resultBox: document.getElementById('aiClassifyResult')
        };
    }

    function setStatus(type, message) {
        const elements = getElements();
        if (!elements.statusBox) {
            return;
        }
        elements.statusBox.className = 'ai-classify-status ' + (type || 'info');
        elements.statusBox.textContent = message || '';
    }

    function setResult(data) {
        const elements = getElements();
        if (!elements.resultBox) {
            return;
        }
        const categoryText = data && data.categoryName ? data.categoryName : 'Chưa xác định';
        const brandText = data && data.brandName ? data.brandName : 'Chưa xác định';
        const confidence = data && typeof data.confidence === 'number'
            ? Math.round(data.confidence * 100) + '%'
            : '0%';
        elements.resultBox.innerHTML =
            '<div><strong>Thể loại:</strong> ' + escapeHtml(categoryText) + '</div>' +
            '<div><strong>Hãng:</strong> ' + escapeHtml(brandText) + '</div>' +
            '<div><strong>Độ tin cậy:</strong> ' + escapeHtml(confidence) + '</div>';
    }

    function escapeHtml(value) {
        return String(value)
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#039;');
    }

    function selectByValue(select, value) {
        if (!select || value === null || value === undefined || value === '') {
            return false;
        }
        const textValue = String(value);
        const option = Array.from(select.options).find(item => item.value === textValue);
        if (!option) {
            return false;
        }
        select.value = textValue;
        select.dispatchEvent(new Event('change', { bubbles: true }));
        return true;
    }

    async function classifySelectedImage(input) {
        const elements = getElements();
        const file = input && input.files && input.files[0];
        if (!file) {
            setStatus('info', 'Chọn ảnh từ máy để AI tự gợi ý thể loại và hãng sản xuất.');
            if (elements.resultBox) {
                elements.resultBox.innerHTML = '';
            }
            return;
        }

        if (!file.type || !file.type.startsWith('image/')) {
            setStatus('error', 'File đã chọn không phải là ảnh hợp lệ.');
            return;
        }

        if (file.size > 5 * 1024 * 1024) {
            setStatus('error', 'Ảnh vượt quá 5MB. Vui lòng chọn ảnh nhỏ hơn.');
            return;
        }

        const endpoint = elements.statusBox && elements.statusBox.dataset.aiUrl
            ? elements.statusBox.dataset.aiUrl
            : '/admin/products/ai-classify';

        const formData = new FormData();
        formData.append('imageFile', file);

        setStatus('loading', 'AI đang phân tích ảnh, vui lòng chờ...');
        if (elements.resultBox) {
            elements.resultBox.innerHTML = '';
        }

        try {
            const response = await fetch(endpoint, {
                method: 'POST',
                body: formData,
                credentials: 'same-origin'
            });

            if (!response.ok) {
                throw new Error('HTTP ' + response.status);
            }

            const data = await response.json();
            if (!data || data.success === false) {
                setStatus('error', data && data.message ? data.message : 'AI chưa phân loại được ảnh này.');
                setResult(data || {});
                return;
            }

            const selectedGenre = selectByValue(elements.genreSelect, data.categoryId);
            const selectedBrand = selectByValue(elements.brandSelect, data.brandId);
            setResult(data);

            if (data.lowConfidence || !selectedGenre || !selectedBrand) {
                setStatus('warning', data.message || 'AI không chắc kết quả, vui lòng kiểm tra lại thể loại và hãng sản xuất.');
            } else {
                setStatus('success', data.message || 'AI đã tự chọn thể loại và hãng sản xuất.');
            }
        } catch (error) {
            setStatus('error', 'Không gọi được API phân loại ảnh: ' + error.message);
        }
    }

    document.addEventListener('DOMContentLoaded', function () {
        const elements = getElements();
        if (!elements.fileInput) {
            return;
        }
        setStatus('info', 'Chọn ảnh từ máy để AI tự gợi ý thể loại và hãng sản xuất.');
        elements.fileInput.addEventListener('change', function () {
            classifySelectedImage(elements.fileInput);
        });
    });
})();
