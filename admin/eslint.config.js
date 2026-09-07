import js from '@eslint/js'
import globals from 'globals'
import reactHooks from 'eslint-plugin-react-hooks'
import reactRefresh from 'eslint-plugin-react-refresh'
import tseslint from 'typescript-eslint'

export default tseslint.config(
  { ignores: ['dist/', 'node_modules/'] },
  {
    extends: [js.configs.recommended, ...tseslint.configs.recommended],
    // 只覆盖 ts/tsx（React 版管理后台）
    files: ['**/*.{ts,tsx}'],
    languageOptions: {
      ecmaVersion: 2020,
      globals: globals.browser,
    },
    plugins: {
      'react-hooks': reactHooks,
      'react-refresh': reactRefresh,
    },
    rules: {
      ...reactHooks.configs.recommended.rules,
      'react-refresh/only-export-components': ['warn', { allowConstantExport: true }],
      // 存量代码风格噪音类规则，作为 CI 质量门先降级为 warn
      '@typescript-eslint/no-explicit-any': 'warn',
      '@typescript-eslint/no-unused-vars': 'warn',
    },
  },
  {
    files: ['vite.config.*', '*.config.js', '*.config.cjs'],
    languageOptions: {
      globals: {
        ...globals.node,
      },
    },
  },
)
