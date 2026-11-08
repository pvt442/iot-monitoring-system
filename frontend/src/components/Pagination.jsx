import { ChevronLeft, ChevronRight } from 'lucide-react';

export default function Pagination({ page, totalPages, totalElements, size, onPage, onSize, sizes = [10, 20, 50, 100] }) {
  const from = totalElements === 0 ? 0 : page * size + 1;
  const to = Math.min(totalElements, (page + 1) * size);
  return (
    <div className="pager">
      <span>
        Hiển thị <b>{from}–{to}</b> / {totalElements.toLocaleString('vi-VN')} bản ghi
      </span>
      <div className="pager-ctrl">
        <select className="select" style={{ width: 'auto', height: 34 }} value={size} aria-label="Số dòng mỗi trang"
          onChange={(e) => onSize(Number(e.target.value))}>
          {sizes.map((s) => <option key={s} value={s}>{s} dòng/trang</option>)}
        </select>
        <button type="button" className="btn btn-icon" disabled={page <= 0} onClick={() => onPage(page - 1)} aria-label="Trang trước">
          <ChevronLeft aria-hidden="true" />
        </button>
        <span>Trang {totalPages === 0 ? 0 : page + 1}/{totalPages}</span>
        <button type="button" className="btn btn-icon" disabled={page + 1 >= totalPages} onClick={() => onPage(page + 1)} aria-label="Trang sau">
          <ChevronRight aria-hidden="true" />
        </button>
      </div>
    </div>
  );
}
