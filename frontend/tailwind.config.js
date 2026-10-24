/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: { brand: { 50: '#eef6f3', 100: '#d5ebe2', 600: '#0f6b4f', 700: '#0b5640', 800: '#08402f' } },
    },
  },
  plugins: [],
};
