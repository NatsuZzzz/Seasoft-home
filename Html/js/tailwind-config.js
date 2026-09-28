// Theme SeaSoft cho Tailwind CDN - nap ngay sau script cdn.tailwindcss.com
tailwind.config = {
  darkMode: "class",
  theme: {
    extend: {
      colors: {
        primary: "#48CAE4",
        secondary: "#90E0EF",
        "dark-navy": "#03045E",
        surface: "#F8FCFF",
        "surface-alt": "#EBF8FC",
        "text-secondary": "#333333",
        "text-muted": "#6B7280",
        border: "#E2E8F0",
      },
      fontFamily: {
        headline: ["Inter", "sans-serif"],
        display: ["Inter", "sans-serif"],
        body: ["Inter", "sans-serif"],
        label: ["Inter", "sans-serif"],
      },
      borderRadius: {
        DEFAULT: "0.25rem",
        sm: "0.25rem",
        md: "0.5rem",
        lg: "0.5rem",
        xl: "0.75rem",
        "2xl": "1rem",
        full: "9999px",
      },
      boxShadow: {
        card: "0 10px 30px -5px rgba(3, 4, 94, 0.05)",
        cardHover: "0 20px 40px -5px rgba(72, 202, 228, 0.18)",
        cyanGlow: "0 0 60px rgba(72, 202, 228, 0.4)",
      },
    },
  },
};
