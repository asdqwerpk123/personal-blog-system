import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import { describe, expect, it } from 'vitest';

describe('primary action button styles', () => {
  it('locks the shared create button appearance against Element Plus defaults', () => {
    const css = readFileSync(resolve(process.cwd(), 'src/styles/index.css'), 'utf8');

    expect(css).toContain('.primary-action-button.el-button');
    expect(css).toContain('height: 44px');
    expect(css).toContain('border-radius: 7px');
    expect(css).toContain('color: #ffffff !important');
    expect(css).toContain('--el-button-text-color: #ffffff');
    expect(css).toContain('--el-button-hover-text-color: #ffffff');
    expect(css).toContain('box-shadow: 0 8px 18px rgba(54, 87, 245, 0.22)');
  });
});
