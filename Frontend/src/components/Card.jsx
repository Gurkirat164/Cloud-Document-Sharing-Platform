// Glassmorphism-style card container component.
export default function Card({ children, className = '', ...props }) {
  return (
    <div
      className={`rounded-xl border border-[#252840] bg-[#1a1d2e] p-6 shadow-lg ${className}`}
      {...props}
    >
      {children}
    </div>
  )
}
