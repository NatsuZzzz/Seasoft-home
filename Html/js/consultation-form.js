/* SeaSoft - form "Nhan tu van mien phi" tren trang chu -> POST /api/consultations */
(function () {
  "use strict";

  const form = document.getElementById("consult-form");
  if (!form) return;

  const btn = document.getElementById("cf-submit");
  const btnText = document.getElementById("cf-submit-text");
  const errorBox = document.getElementById("cf-error");
  const success = document.getElementById("cf-success");
  const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  const PHONE_RE = /^\+?[0-9 .-]{8,20}$/;

  // Da dang nhap -> dien san ten, email, SDT
  if (window.Auth?.isLoggedIn()) {
    Auth.api("/api/users/me")
      .then((me) => {
        if (!form.fullName.value) form.fullName.value = me.fullName || "";
        if (!form.email.value) form.email.value = me.email || "";
        if (!form.phone.value) form.phone.value = me.phone || "";
      })
      .catch(() => {});
  }

  function showFieldErrors(errors) {
    form.querySelectorAll("[data-error-for]").forEach((p) => {
      const msg = errors[p.dataset.errorFor];
      p.textContent = msg || "";
      p.classList.toggle("hidden", !msg);
      form.elements[p.dataset.errorFor]?.classList.toggle("border-red-400", !!msg);
    });
  }

  function validate(data) {
    const e = {};
    if (!data.fullName) e.fullName = "Vui lòng nhập họ tên";
    if (!PHONE_RE.test(data.phone)) e.phone = "Số điện thoại không hợp lệ";
    if (!EMAIL_RE.test(data.email)) e.email = "Email không hợp lệ";
    if (!data.serviceType) e.serviceType = "Vui lòng chọn dịch vụ";
    return e;
  }

  function shake(el) {
    el.classList.remove("shake");
    void el.offsetWidth;
    el.classList.add("shake");
  }

  form.addEventListener("submit", async (ev) => {
    ev.preventDefault();
    errorBox.classList.add("hidden");
    const data = {
      fullName: form.fullName.value.trim(),
      phone: form.phone.value.trim(),
      email: form.email.value.trim(),
      companyName: form.companyName.value.trim(),
      serviceType: form.serviceType.value,
      budgetRange: form.budgetRange.value,
      message: form.message.value.trim(),
      website: form.website.value, // honeypot
    };

    const errors = validate(data);
    showFieldErrors(errors);
    if (Object.keys(errors).length) {
      shake(form);
      form.elements[Object.keys(errors)[0]]?.focus();
      return;
    }

    btn.disabled = true;
    btnText.textContent = "Đang gửi…";
    try {
      // Dang nhap -> gui kem token de gan yeu cau vao tai khoan
      const res = Auth.isLoggedIn()
        ? await Auth.api("/api/consultations", { method: "POST", body: data })
        : await Auth.request("/api/consultations", { method: "POST", body: data });
      document.getElementById("cf-success-text").textContent = res.message;
      form.classList.add("hidden");
      success.classList.remove("hidden");
      success.classList.add("pop-in");
    } catch (err) {
      showFieldErrors(err.errors || {});
      errorBox.textContent = err.message;
      errorBox.classList.remove("hidden");
      shake(form);
    } finally {
      btn.disabled = false;
      btnText.textContent = "Nhận tư vấn miễn phí";
    }
  });

  document.getElementById("cf-again").addEventListener("click", () => {
    form.companyName.value = "";
    form.message.value = "";
    form.serviceType.value = "";
    success.classList.add("hidden");
    form.classList.remove("hidden");
    form.classList.add("pop-in");
  });
})();
