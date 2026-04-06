// Shared Button component — supports primary, secondary, and danger variants.
export default function Button({ children, variant = 'primary', className = '', ...props }) {
  const base = 'inline-flex items-center justify-center gap-2 font-medium rounded-lg px-4 py-2 transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed'
  const variants = {
    primary:   'bg-[#6c63ff] text-white hover:bg-[#5a52e0] shadow',
    secondary: 'bg-[#1a1d2e] text-white border border-[#252840] hover:border-[#6c63ff]',
    danger:    'bg-[#ff4d6d] text-white hover:bg-[#e03b5a]',
  }
  return (
    <button className={`${base} ${variants[variant]} ${className}`} {...props}>
      {children}
    </button>
  )
}
