document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('.ajax-add-to-cart-form').forEach(form => {
        form.addEventListener('submit', async function (event) {
            event.preventDefault();

            const button = form.querySelector('button[type="submit"]');
            const originalText = button ? button.textContent.trim() : '';

            try {
                if (button) {
                    button.disabled = true;
                    button.textContent = 'Đang thêm...';
                }

                const response = await fetch(form.action, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded',
                        'X-Requested-With': 'XMLHttpRequest'
                    },
                    body: new URLSearchParams(new FormData(form))
                });

                if (!response.ok) {
                    throw new Error('Không thể thêm sản phẩm vào giỏ hàng');
                }

                const result = await response.json();

                if (result.success) {
                    updateCartCount(result.cartCount);

                    if (button) {
                        button.textContent = 'Đã thêm ✓';

                        setTimeout(() => {
                            button.textContent = originalText;
                            button.disabled = false;
                        }, 900);
                    }
                }
            } catch (error) {
                alert(error.message || 'Có lỗi xảy ra khi thêm vào giỏ hàng.');

                if (button) {
                    button.textContent = originalText;
                    button.disabled = false;
                }
            }
        });
    });
});

function updateCartCount(cartCount) {
    const cartCountBadge = document.getElementById('cartCountBadge');

    if (!cartCountBadge) {
        return;
    }

    cartCountBadge.textContent = ' (' + cartCount + ')';

    if (cartCount > 0) {
        cartCountBadge.classList.remove('d-none');
    } else {
        cartCountBadge.classList.add('d-none');
    }
}