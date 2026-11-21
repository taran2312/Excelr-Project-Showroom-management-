/**
 * Label + Bootstrap control. `as` = "input" (default) | "select" | "textarea".
 * For selects, pass <option> elements as children.
 */
export default function FormField({ label, name, as = 'input', required, col = 'col-md-6', children, ...props }) {
  const id = `f-${name}`;
  let control;
  if (as === 'textarea') {
    control = <textarea id={id} name={name} className="form-control" rows={3} required={required} {...props} />;
  } else if (as === 'select') {
    control = (
      <select id={id} name={name} className="form-select" required={required} {...props}>
        {children}
      </select>
    );
  } else {
    control = <input id={id} name={name} className="form-control" required={required} {...props} />;
  }
  return (
    <div className={col}>
      <label htmlFor={id} className="form-label">
        {label}
        {required && <span className="text-danger ms-1">*</span>}
      </label>
      {control}
    </div>
  );
}
